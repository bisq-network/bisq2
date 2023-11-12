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

package bisq.network.p2p.services.peergroup.exchange;

import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peergroup.Peer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static bisq.network.NetworkService.NETWORK_IO_POOL;
import static java.util.concurrent.CompletableFuture.supplyAsync;

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
    private int doInitialPeerExchangeDelaySec = 1; //todo move to config
    private volatile boolean isStopped;
    private CompletableFuture<Void> resultFuture;
    private Optional<Scheduler> scheduler = Optional.empty();

    public PeerExchangeService(Node node, PeerExchangeStrategy peerExchangeStrategy) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;
        this.node.addListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof PeerExchangeRequest) {
            PeerExchangeRequest request = (PeerExchangeRequest) envelopePayloadMessage;
            //log.debug("Node {} received PeerExchangeRequest with myPeers {}", node, request.peers());
            Address peerAddress = connection.getPeerAddress();
            List<Peer> myPeers = new ArrayList<>(peerExchangeStrategy.getPeersForReporting(peerAddress));
            peerExchangeStrategy.addReportedPeers(new HashSet<>(request.getPeers()), peerAddress);
            NETWORK_IO_POOL.submit(() -> node.send(new PeerExchangeResponse(request.getNonce(), myPeers), connection));
            log.debug("Node {} sent PeerExchangeResponse with my myPeers {}", node, myPeers);
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

    public CompletableFuture<Void> startInitialPeerExchange() {
        return doPeerExchange(peerExchangeStrategy.getAddressesForInitialPeerExchange());
    }

    public void startFurtherPeerExchange() {
        doPeerExchange(peerExchangeStrategy.getAddressesForFurtherPeerExchange());
    }

    /**
     * If the futures of all candidates are completed, we check if we need to redo the peer exchange, and if so, we
     * repeat with increasing delay (up to 20 sec).
     *
     * @param candidates The addresses to use for peer exchange.
     * @return A CompletableFuture which completes if at least one peer exchange was successful or
     * if all failed (expected if no peers are in the network).
     */
    private CompletableFuture<Void> doPeerExchange(List<Address> candidates) {
        if (candidates.isEmpty() || isStopped) {
            return CompletableFuture.completedFuture(null);
        }

        if (resultFuture != null && !resultFuture.isDone()) {
            log.warn("We have a not completed future. We drop that call and return.");
            return CompletableFuture.failedFuture(new RuntimeException("We got called doPeerExchange while the previous future " +
                    "was not completed"));
        }

        log.info("Node {} starts peer exchange with: {}", node,
                StringUtils.truncate(candidates.stream()
                        .map(Address::toString)
                        .collect(Collectors.toList())
                        .toString(), 100));

        scheduler.ifPresent(Scheduler::stop);
        resultFuture = new CompletableFuture<>();
        AtomicInteger numSuccess = new AtomicInteger();
        AtomicInteger numFailures = new AtomicInteger();
        candidates.stream()
                .map(this::doPeerExchangeAsync)
                .forEach(future -> {
                    future.whenComplete((result, throwable) -> {
                        if (throwable == null && result) {
                            numSuccess.incrementAndGet();
                            if (!resultFuture.isDone()) {
                                log.info("We got at least one peerExchange future completed.");
                                resultFuture.complete(null);
                            }
                        } else {
                            numFailures.incrementAndGet();
                        }

                        if (numFailures.get() == candidates.size() && !resultFuture.isDone()) {
                            log.info("We got all peerExchange futures completed but none was successful. " +
                                    "This is expected when no other peers are in the network");
                            resultFuture.complete(null);
                        }

                        if (numFailures.get() + numSuccess.get() == candidates.size()) {
                            log.info("Node {} completed peer exchange to {} candidates. {} requests successfully completed.",
                                    node, candidates.size(), numSuccess);
                            if (peerExchangeStrategy.shouldRedoInitialPeerExchange(numSuccess.get(), candidates.size())) {
                                log.info("Node {} repeats the initial peer exchange after {} sec as it has not reached sufficient connections " +
                                        "or received sufficient peers", node, doInitialPeerExchangeDelaySec);
                                scheduler.ifPresent(Scheduler::stop);
                                scheduler = Optional.of(Scheduler.run(this::startInitialPeerExchange)
                                        .after(doInitialPeerExchangeDelaySec, TimeUnit.SECONDS)
                                        .name("PeerExchangeService.scheduler-" + StringUtils.truncate(node.toString(), 10)));
                                doInitialPeerExchangeDelaySec = Math.min(20, doInitialPeerExchangeDelaySec * 2);
                            } else {
                                log.info("We have completed our peer exchange as we have sufficient connections established.");
                                scheduler.ifPresent(Scheduler::stop);
                                scheduler = Optional.empty();
                            }
                        }
                    });
                });
        return resultFuture;
    }

    private CompletableFuture<Boolean> doPeerExchangeAsync(Address peerAddress) {
        return supplyAsync(() -> doPeerExchange(peerAddress), NETWORK_IO_POOL);
    }

    private boolean doPeerExchange(Address peerAddress) {
        String key = null;
        try {
            Connection connection = node.getConnection(peerAddress);
            key = connection.getId();
            if (requestHandlerMap.containsKey(key)) {
                log.info("requestHandlerMap contains {}. " +
                                "This can happen if the connection is still pending the response or the peer is not available " +
                                "but the timeout has not triggered an exception yet. We consider the past request as failed. Connection={}",
                        key, connection);

                requestHandlerMap.get(key).dispose();
                requestHandlerMap.remove(key);
                return false;
            }

            PeerExchangeRequestHandler handler = new PeerExchangeRequestHandler(node, connection);
            requestHandlerMap.put(key, handler);
            Set<Peer> myPeers = peerExchangeStrategy.getPeersForReporting(peerAddress);

            // We request and wait for response
            Set<Peer> reportedPeers = handler.request(myPeers).join();
            log.info("Node {} completed peer exchange with {} and received {} reportedPeers.",
                    node, peerAddress, reportedPeers.size());
            peerExchangeStrategy.addReportedPeers(reportedPeers, peerAddress);
            requestHandlerMap.remove(key);
            return true;
        } catch (Throwable throwable) {
            if (key != null) {
                if (requestHandlerMap.containsKey(key)) {
                    requestHandlerMap.get(key).dispose();
                    requestHandlerMap.remove(key);
                }
            }
            log.debug("Node {} failed to do a peer exchange with {}.",
                    node, peerAddress, throwable);
            return false;
        }
    }

    public void shutdown() {
        isStopped = true;
        scheduler.ifPresent(Scheduler::stop);
        scheduler = Optional.empty();
        requestHandlerMap.values().forEach(PeerExchangeRequestHandler::dispose);
        requestHandlerMap.clear();
        peerExchangeStrategy.shutdown();
    }
}