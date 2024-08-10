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
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.Peer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Getter
public class PeerExchangeAttempt implements Node.Listener {
    private static final int TIMEOUT_SEC = 90;

    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;
    private final String name;

    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final AtomicInteger numSuccess = new AtomicInteger();
    private final AtomicInteger numFailures = new AtomicInteger();
    private final CountDownLatch timoutLatch = new CountDownLatch(1);
    private final CompletableFuture<Void> peerExchangeFuture = new CompletableFuture<>();
    private final AtomicBoolean requireRetry = new AtomicBoolean();
    private final AtomicReference<Observable<Boolean>> minSuccessReached = new AtomicReference<>(new Observable<>(false));
    private final AtomicReference<Observable<Boolean>> completed = new AtomicReference<>(new Observable<>(false));
    private final AtomicBoolean isShutdownInProgress = new AtomicBoolean();

    public PeerExchangeAttempt(Node node, PeerExchangeStrategy peerExchangeStrategy, String name) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;
        this.name = name;
        this.node.addListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof PeerExchangeRequest request) {
            Address peerAddress = connection.getPeerAddress();
            List<Peer> myPeers = new ArrayList<>(peerExchangeStrategy.getPeersForReporting(peerAddress));
            peerExchangeStrategy.addReportedPeers(new HashSet<>(request.getPeers()), peerAddress);
            NETWORK_IO_POOL.submit(() -> node.send(new PeerExchangeResponse(request.getNonce(), myPeers), connection));
            log.debug("Sent PeerExchangeResponse with my myPeers {}", myPeers);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Void> start(int minSuccess, List<Address> candidates) {
        checkArgument(!completed.get().get(), "Already completed");
        checkArgument(!isShutdownInProgress.get(), "Already shutdown");
        checkArgument(minSuccess > 0, "minSuccess must be > 0");
        checkArgument(!candidates.isEmpty(), "Candidates must not be empty");

        log.info("{} candidates for peerExchange {}. At instance: {}",
                candidates.size(),
                StringUtils.truncate(candidates.stream()
                        .map(Address::toString)
                        .toList()
                        .toString(), 2000), this);

        AtomicInteger numMinSuccess = new AtomicInteger(Math.min(minSuccess, candidates.size()));
        candidates.stream()
                .map(this::doPeerExchangeAsync)
                .forEach(future -> future.whenComplete((nil, throwable) -> onFutureComplete(throwable, numMinSuccess, candidates.size())));
        try {
            boolean hadTimeout = !timoutLatch.await(TIMEOUT_SEC, SECONDS);
            if (hadTimeout) {
                log.warn("Peer exchange not completed in {} seconds. At instance: {}", TIMEOUT_SEC, this);
                peerExchangeFuture.completeExceptionally(new TimeoutException("Peer exchange not completed in " + TIMEOUT_SEC + " seconds."));
                shutdown();
            }
        } catch (Exception e) {
            log.warn("timoutLatch.await failed. {}. At instance: {}", ExceptionUtil.getRootCauseMessage(e), this);
            peerExchangeFuture.completeExceptionally(new RuntimeException("timoutLatch.await failed.", e));
            shutdown();
        }

        return peerExchangeFuture;
    }

    public void shutdown() {
        log.debug("shutdown at instance: {}", this);
        if (isShutdownInProgress.get()) {
            return;
        }
        this.node.removeListener(this);
        isShutdownInProgress.set(true);
        requestHandlerMap.values().forEach(PeerExchangeRequestHandler::dispose);
        requestHandlerMap.clear();
        if (timoutLatch.getCount() > 0) {
            timoutLatch.countDown();
        }
        if (!peerExchangeFuture.isDone()) {
            peerExchangeFuture.cancel(true);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onFutureComplete(Throwable throwable, AtomicInteger numMinSuccess, int candidatesSize) {
        if (isShutdownInProgress.get()) {
            return;
        }
        if (throwable == null) {
            numSuccess.incrementAndGet();
        } else {
            numFailures.incrementAndGet();
        }

        boolean isMinSuccessReached = numSuccess.get() >= numMinSuccess.get();
        if (!minSuccessReached.get().get()) {
            minSuccessReached.get().set(isMinSuccessReached);
            if (isMinSuccessReached && timoutLatch.getCount() > 0) {
                log.info("Min. success reached at initial peer exchange.\nnumSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}. At instance: {}",
                        numSuccess.get(), numFailures.get(), numMinSuccess.get(), candidatesSize, this);
                timoutLatch.countDown();
                // Min success reached, we set the result at the peerExchangeFuture
                peerExchangeFuture.complete(null);
            }
        }

        boolean allCompleted = numFailures.get() + numSuccess.get() == candidatesSize;
        if (allCompleted) {
            log.info("Peer exchange completed. numSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}. At instance: {}",
                    numSuccess.get(), numFailures.get(), numMinSuccess.get(), candidatesSize, this);

            boolean tooManyFailures = peerExchangeStrategy.tooManyFailures(numSuccess.get(), numFailures.get());
            boolean needsMoreConnections = peerExchangeStrategy.needsMoreConnections();
            boolean needsMoreReportedPeers = peerExchangeStrategy.needsMoreReportedPeers();
            if (tooManyFailures || needsMoreConnections || needsMoreReportedPeers) {
                requireRetry.set(true);
                if (tooManyFailures) {
                    log.warn("Require retry of peer exchange because of: tooManyFailures. At instance: {}", this);
                } else if (needsMoreConnections) {
                    log.warn("Require retry of peer exchange because of: needsMoreConnections. At instance: {}", this);
                } else {
                    log.warn("Require retry of peer exchange because of: needsMoreReportedPeers. At instance: {}", this);
                }
            } else {
                log.info("We stop our peer exchange as we have sufficient connections established. At instance: {}", this);
            }

            if (!isMinSuccessReached) {
                log.warn("All peer exchange completed but minSuccessReached not reached.\nnumSuccess={}; numFailures={}; numMinSuccess.get()={}; candidates.size()={}. At instance: {}",
                        numSuccess.get(), numFailures.get(), numMinSuccess.get(), candidatesSize, this);
                requireRetry.set(true);
                peerExchangeFuture.completeExceptionally(new RuntimeException("All peer exchange completed but minSuccessReached not reached."));
            }

            if (!completed.get().get()) {
                completed.get().set(true);
                shutdown();
            }
        }
    }

    private CompletableFuture<Void> doPeerExchangeAsync(Address peerAddress) {
        return supplyAsync(() -> {
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
                log.debug("Completed peer exchange with {} and received {} reportedPeers. At instance: {}", peerAddress, reportedPeers.size(), this);
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