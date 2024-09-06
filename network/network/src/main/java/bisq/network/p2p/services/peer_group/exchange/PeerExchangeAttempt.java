/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.p2p.services.peer_group.exchange;

import bisq.common.observable.Observable;
import bisq.common.threading.ThreadName;
import bisq.common.util.ExceptionUtil;
import bisq.network.common.Address;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
@Getter
public class PeerExchangeAttempt {
    private static final int TIMEOUT_SEC = 90;

    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;
    private final String name;

    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final AtomicInteger numSuccess = new AtomicInteger();
    private final AtomicInteger numFailures = new AtomicInteger();
    private final CompletableFuture<Boolean> peerExchangeFuture = new CompletableFuture<>();
    private final AtomicReference<Observable<Boolean>> minSuccessReached = new AtomicReference<>(new Observable<>(false));
    private final AtomicBoolean isShutdownInProgress = new AtomicBoolean();

    public PeerExchangeAttempt(Node node, PeerExchangeStrategy peerExchangeStrategy, String name) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;
        this.name = name;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> startAsync(int minSuccess, List<Address> candidates) {
        checkArgument(!peerExchangeFuture.isDone(), "peerExchangeFuture already done");
        checkArgument(!isShutdownInProgress.get(), "Already shutdown");
        checkArgument(!candidates.isEmpty(), "Candidates must not be empty");

        log.info("Do peer exchange with {} candidates at instance: {}\n{}",
                candidates.size(), this, Joiner.on("\n").join(candidates));

        peerExchangeFuture
                .orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .whenComplete((r, t) -> shutdown());

        AtomicInteger numMinSuccess = new AtomicInteger(Math.min(minSuccess, candidates.size()));
        candidates.stream()
                .map(this::doPeerExchangeAsync)
                .forEach(future -> future.whenComplete((nil, throwable) -> onPeerExchangeComplete(throwable, numMinSuccess, candidates.size())));

        return peerExchangeFuture;
    }

    public void shutdown() {
        log.debug("shutdown at instance: {}", this);
        if (isShutdownInProgress.get()) {
            return;
        }

        isShutdownInProgress.set(true);
        requestHandlerMap.values().forEach(PeerExchangeRequestHandler::dispose);
        requestHandlerMap.clear();
        if (!peerExchangeFuture.isDone()) {
            peerExchangeFuture.cancel(true);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onPeerExchangeComplete(Throwable throwable, AtomicInteger numMinSuccess, int candidatesSize) {
        if (isShutdownInProgress.get()) {
            return;
        }
        if (throwable == null) {
            numSuccess.incrementAndGet();
        } else {
            numFailures.incrementAndGet();
        }

        boolean isMinSuccessReached = numSuccess.get() >= numMinSuccess.get();
        if (isMinSuccessReached && !minSuccessReached.get().get()) {
            log.info("Min. success reached at initial peer exchange.\nnumSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}. At instance: {}",
                    numSuccess.get(), numFailures.get(), numMinSuccess.get(), candidatesSize, this);
            minSuccessReached.get().set(true);
        }

        boolean allCompleted = numFailures.get() + numSuccess.get() == candidatesSize;
        if (allCompleted) {
            log.info("Peer exchange completed. numSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}. At instance: {}",
                    numSuccess.get(), numFailures.get(), numMinSuccess.get(), candidatesSize, this);

            boolean tooManyFailures = peerExchangeStrategy.tooManyFailures(numSuccess.get(), numFailures.get());
            boolean needsMoreConnections = peerExchangeStrategy.needsMoreConnections();
            boolean needsMoreReportedPeers = peerExchangeStrategy.needsMoreReportedPeers();
            boolean requireRetry = tooManyFailures || needsMoreConnections || needsMoreReportedPeers || !isMinSuccessReached;
            if (requireRetry) {
                if (tooManyFailures) {
                    log.warn("Require retry of peer exchange because of: tooManyFailures. At instance: {}", this);
                } else if (needsMoreConnections) {
                    log.warn("Require retry of peer exchange because of: needsMoreConnections. At instance: {}", this);
                } else {
                    log.warn("Require retry of peer exchange because of: needsMoreReportedPeers. At instance: {}", this);
                }
                if (!isMinSuccessReached) {
                    log.warn("All peer exchange completed but minSuccessReached not reached.\n" +
                                    "numSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}. At instance: {}",
                            numSuccess.get(), numFailures.get(), numMinSuccess.get(), candidatesSize, this);
                }
            } else {
                log.info("We stop our peer exchange as we have sufficient connections established. At instance: {}", this);
            }

            peerExchangeFuture.complete(!requireRetry);
        }
    }

    private CompletableFuture<Void> doPeerExchangeAsync(Address peerAddress) {
        return supplyAsync(() -> {
            ThreadName.set(this, "request");
            String connectionId = null;
            try {
                Connection connection = node.getConnection(peerAddress);
                connectionId = connection.getId();
                checkArgument(!requestHandlerMap.containsKey(connectionId), "We have a pending request for that connection");

                PeerExchangeRequestHandler handler = new PeerExchangeRequestHandler(node, connection);
                requestHandlerMap.put(connectionId, handler);
                Set<Peer> myPeers = peerExchangeStrategy.getPeersForReporting(peerAddress);

                // We request and wait blocking for response
                Set<Peer> reportedPeers = handler.request(myPeers).join();
                log.info("Completed peer exchange with {} and received {} reportedPeers. At instance: {}", peerAddress, reportedPeers.size(), this);
                peerExchangeStrategy.addReportedPeers(reportedPeers, peerAddress);
                requestHandlerMap.remove(connectionId);
                return null;
            } catch (Exception exception) {
                if (!isShutdownInProgress.get()) {
                    log.warn("Peer exchange with {} failed. {}. At instance: {}", peerAddress, ExceptionUtil.getRootCauseMessage(exception), this);
                }
                if (connectionId != null) {
                    if (requestHandlerMap.containsKey(connectionId)) {
                        requestHandlerMap.get(connectionId).dispose();
                        requestHandlerMap.remove(connectionId);
                    }
                }
                throw exception;
            }
        }, NETWORK_IO_POOL);
    }

    @Override
    public String toString() {
        return name + " " + "@" + Integer.toHexString(hashCode());
    }
}