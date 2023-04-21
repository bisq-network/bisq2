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
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peergroup.Peer;
import bisq.network.p2p.services.peergroup.PersistedPeersHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static bisq.network.NetworkService.NETWORK_IO_POOL;

/**
 * Responsible for executing the peer exchange protocol with set of peers.
 * We use the PeerExchangeStrategy for the selection of nodes.
 */
@Slf4j
@Getter
public class PeerExchangeService implements Node.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;

    // todo if persisted peer needs to be written from that class we can use the addPersistedPeerHandler to delegate it 
    // to the PeerGroupService. We do not want a dependency from PeerExchangeService to PeerGroupService as 
    // PeerExchangeService got created by PeerGroupService
    private final PersistedPeersHandler persistedPeersHandler;
    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private int doInitialPeerExchangeDelaySec = 1; //todo move to config
    private volatile boolean isStopped;
    private Optional<Scheduler> scheduler = Optional.empty();

    public PeerExchangeService(Node node, PeerExchangeStrategy peerExchangeStrategy, PersistedPeersHandler persistedPeersHandler) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;
        this.persistedPeersHandler = persistedPeersHandler;
        this.node.addListener(this);
    }

    public CompletableFuture<Void> doInitialPeerExchange() {
        return doPeerExchange(peerExchangeStrategy.getAddressesForInitialPeerExchange());
    }

    public CompletableFuture<Void> doFurtherPeerExchange() {
        return doPeerExchange(peerExchangeStrategy.getAddressesForFurtherPeerExchange());
    }

    private CompletableFuture<Void> doPeerExchange(Set<Address> candidates) {
        if (candidates.isEmpty() || isStopped) {
            return CompletableFuture.completedFuture(null);
        }
        log.info("Node {} starts peer exchange with: {}", node,
                StringUtils.truncate(candidates.stream()
                        .map(Address::toString)
                        .collect(Collectors.toList())
                        .toString()));

        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        AtomicInteger numSuccess = new AtomicInteger();
        AtomicInteger numFailures = new AtomicInteger();
        candidates.stream()
                .map(this::doPeerExchangeAsync)
                .forEach(future -> {
                    future.whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            if (result) {
                                numSuccess.incrementAndGet();
                                if (!resultFuture.isDone()) {
                                    log.info("We got at least one peerExchange future completed.");
                                    resultFuture.complete(null);
                                }
                            } else {
                                numFailures.incrementAndGet();
                            }
                        } else {
                            numFailures.incrementAndGet();
                        }

                        if (numFailures.get() + numSuccess.get() == candidates.size()) {
                            if (!resultFuture.isDone()) {
                                log.info("We got all peerExchange futures completed but none was successful. This is expected when the first node bootstraps");
                                resultFuture.complete(null);
                            }

                            log.info("Node {} completed peer exchange to {} candidates. {} requests successfully completed.",
                                    node, candidates.size(), numSuccess);
                            if (peerExchangeStrategy.shouldRedoInitialPeerExchange(numSuccess.get(), candidates.size())) {
                                log.info("Node {} repeats the initial peer exchange after {} sec as it has not reached sufficient connections " +
                                        "or received sufficient peers", node, doInitialPeerExchangeDelaySec);
                                scheduler.ifPresent(Scheduler::stop);
                                scheduler = Optional.of(Scheduler.run(this::doInitialPeerExchange)
                                        .after(doInitialPeerExchangeDelaySec, TimeUnit.SECONDS)
                                        .name("PeerExchangeService.scheduler-" + node));
                                doInitialPeerExchangeDelaySec = Math.min(60, doInitialPeerExchangeDelaySec * 2);
                            } else {
                                scheduler.ifPresent(Scheduler::stop);
                            }
                        }
                    });
                });

        return resultFuture;
    }

    private CompletableFuture<Boolean> doPeerExchangeAsync(Address peerAddress) {
        return node.getConnectionAsync(peerAddress)
                .handleAsync((BiFunction<Connection, Throwable, Boolean>) (connection, throwable) -> {
                    String key = connection != null ? connection.getId() : null;

                    if (throwable != null) {
                        if (key != null) {
                            requestHandlerMap.remove(key);
                        }
                        log.info("Node {} failed to do a peer exchange with {} because of: {}", node, peerAddress, throwable.getMessage());
                        return false;
                    }

                    if (connection == null) {
                        return false;
                    }

                    if (requestHandlerMap.containsKey(key)) {
                        log.warn("Node {} : requestHandlerMap contains already {}. " +
                                "We dispose the existing handler and start a new one.", node, peerAddress);
                        requestHandlerMap.get(key).dispose();
                    }

                    PeerExchangeRequestHandler handler = new PeerExchangeRequestHandler(node, connection);
                    requestHandlerMap.put(key, handler);
                    Set<Peer> myPeers = peerExchangeStrategy.getPeers(peerAddress);

                    Set<Peer> peers = handler.request(myPeers).join();
                    log.info("Node {} completed peer exchange with {} and received {} peers.", node, peerAddress, peers.size());
                    peerExchangeStrategy.addReportedPeers(peers, peerAddress);
                    requestHandlerMap.remove(key);
                    return true;

                }, NETWORK_IO_POOL);
    }

    public void shutdown() {
        isStopped = true;
        scheduler.ifPresent(Scheduler::stop);
        requestHandlerMap.values().forEach(PeerExchangeRequestHandler::dispose);
        requestHandlerMap.clear();
        peerExchangeStrategy.shutdown();
    }

    @Override
    public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
        if (networkMessage instanceof PeerExchangeRequest) {
            PeerExchangeRequest request = (PeerExchangeRequest) networkMessage;
            //log.debug("Node {} received PeerExchangeRequest with myPeers {}", node, request.peers());
            Address peerAddress = connection.getPeerAddress();
            peerExchangeStrategy.addReportedPeers(new HashSet<>(request.getPeers()), peerAddress);
            List<Peer> myPeers = new ArrayList<>(peerExchangeStrategy.getPeers(peerAddress));
            NETWORK_IO_POOL.submit(() -> node.sendAsync(new PeerExchangeResponse(request.getNonce(), myPeers), connection));
            log.debug("Node {} sent PeerExchangeResponse with my myPeers {}", node, myPeers);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }
}