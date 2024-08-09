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

import bisq.common.timer.Scheduler;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Responsible for executing the peer exchange protocol with set of peers.
 * We use the PeerExchangeStrategy for the selection of nodes.
 */
@Slf4j
@Getter
public class PeerExchangeService implements Node.Listener {
    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;
    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private int peerExchangeDelaySec = 1;
    private volatile boolean isShutdownInProgress;
    private volatile boolean initialPeerExchangeCompleted;
    private Optional<Scheduler> peerExchangeScheduler = Optional.empty();

    public PeerExchangeService(Node node, PeerExchangeStrategy peerExchangeStrategy) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;
        this.node.addListener(this);
    }

    public void shutdown() {
        isShutdownInProgress = true;
        peerExchangeScheduler.ifPresent(Scheduler::stop);
        requestHandlerMap.values().forEach(PeerExchangeRequestHandler::dispose);
        requestHandlerMap.clear();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof PeerExchangeRequest) {
            PeerExchangeRequest request = (PeerExchangeRequest) envelopePayloadMessage;
            //log.debug("{} received PeerExchangeRequest with myPeers {}", node, request.peers());
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

    public void startInitialPeerExchange(int minSuccess) {
        if (initialPeerExchangeCompleted) {
            return;
        }
        log.info("startInitialPeerExchange");
        List<Address> candidates = peerExchangeStrategy.getAddressesForInitialPeerExchange();
        doPeerExchange(candidates, minSuccess);
    }

    public void extendPeerGroup() {
        if (!initialPeerExchangeCompleted) {
            return;
        }
        log.info("extendPeerGroup");
        List<Address> candidates = peerExchangeStrategy.getAddressesForExtendingPeerGroup();
        doPeerExchange(candidates, 1);
    }

    private void doPeerExchange(List<Address> candidates, int minSuccess) {
        checkArgument(minSuccess > 0, "minSuccess must be > 0");
        if (isShutdownInProgress) {
            return;
        }

        if (candidates.isEmpty()) {
            return;
        }

        log.info("candidates for doPeerExchange {}",
                StringUtils.truncate(candidates.stream()
                        .map(Address::toString)
                        .collect(Collectors.toList())
                        .toString(), 2000));

        AtomicInteger numMinSuccess = new AtomicInteger(Math.min(minSuccess, candidates.size()));
        AtomicInteger numSuccess = new AtomicInteger();
        AtomicInteger numFailures = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        candidates.stream()
                .map(this::doPeerExchangeAsync)
                .forEach(future -> {
                    future.whenComplete((nil, throwable) -> {
                        if (isShutdownInProgress) {
                            return;
                        }
                        if (throwable == null) {
                            numSuccess.incrementAndGet();
                        } else {
                            numFailures.incrementAndGet();
                        }

                        boolean allCompleted = numFailures.get() + numSuccess.get() == candidates.size();
                        if (allCompleted || numSuccess.get() == numMinSuccess.get()) {
                            log.info("Peer exchange completed. numSuccess={}; numFailures={}",
                                    numSuccess.get(), numFailures.get());
                            latch.countDown();
                        }

                        if (allCompleted) {
                            boolean tooManyFailures = peerExchangeStrategy.tooManyFailures(numSuccess.get(), numFailures.get());
                            boolean needsMoreConnections = peerExchangeStrategy.needsMoreConnections();
                            boolean needsMoreReportedPeers = peerExchangeStrategy.needsMoreReportedPeers();
                            if (tooManyFailures || needsMoreConnections || needsMoreReportedPeers) {
                                if (tooManyFailures) {
                                    log.info("Repeat initial peer exchange after {} sec. Reason: tooManyFailures", peerExchangeDelaySec);
                                } else if (needsMoreConnections) {
                                    log.info("Repeat initial peer exchange after {} sec. Reason: needsMoreConnections", peerExchangeDelaySec);
                                } else {
                                    log.info("Repeat initial peer exchange after {} sec. Reason: needsMoreReportedPeers", peerExchangeDelaySec);
                                }
                                peerExchangeScheduler.ifPresent(Scheduler::stop);
                                peerExchangeScheduler = Optional.of(Scheduler.run(() -> startInitialPeerExchange(minSuccess))
                                        .after(peerExchangeDelaySec, TimeUnit.SECONDS)
                                        .name("PeerExchangeService.scheduler"));
                                peerExchangeDelaySec = Math.min(20, peerExchangeDelaySec * 2);
                            } else {
                                log.info("We stop our peer exchange as we have sufficient connections established.");
                                initialPeerExchangeCompleted = true;
                                peerExchangeScheduler.ifPresent(Scheduler::stop);
                                peerExchangeScheduler = Optional.empty();
                            }
                        }
                    });
                });
        try {
            boolean await = latch.await(30, SECONDS);
            checkArgument(await, "CountDownLatch not completed in 30 seconds");
        } catch (Exception e) {
            log.warn("Error at CountDownLatch.await: {}. Repeat initial peer exchange with cleared persisted and reported peers after {} sec.",
                    ExceptionUtil.getRootCauseMessage(e), peerExchangeDelaySec);
            peerExchangeStrategy.clearPersistedPeers();
            peerExchangeStrategy.clearReportedPeers();
            peerExchangeScheduler.ifPresent(Scheduler::stop);
            peerExchangeScheduler = Optional.of(Scheduler.run(() -> startInitialPeerExchange(minSuccess))
                    .after(peerExchangeDelaySec, TimeUnit.SECONDS)
                    .name("PeerExchangeService.scheduler-" + StringUtils.truncate(node.toString(), 10)));
            peerExchangeDelaySec = Math.min(20, peerExchangeDelaySec * 2);
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

                // We request and wait for response
                Set<Peer> reportedPeers = handler.request(myPeers).join();
                log.info("Completed peer exchange with {} and received {} reportedPeers.",
                        peerAddress, reportedPeers.size());
                peerExchangeStrategy.addReportedPeers(reportedPeers, peerAddress);
                requestHandlerMap.remove(connectionId);
                log.info("Peer exchange with {} completed", peerAddress);
                return null;
            } catch (Exception exception) {
                if (!isShutdownInProgress) {
                    log.info("Peer exchange with {} failed", peerAddress);
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
}