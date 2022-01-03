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

package network.misq.network.p2p.services.peergroup.exchange;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.timer.Scheduler;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.common.util.StringUtils;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.Peer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static network.misq.network.NetworkService.NETWORK_IO_POOL;

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
    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private int doInitialPeerExchangeDelaySec = 1; //todo move to config
    private volatile boolean isStopped;
    private Optional<Scheduler> scheduler = Optional.empty();

    public PeerExchangeService(Node node, PeerExchangeStrategy peerExchangeStrategy) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;
        this.node.addListener(this);
    }

    public CompletableFuture<Void> doInitialPeerExchange() {
        return doPeerExchange(peerExchangeStrategy.getAddressesForInitialPeerExchange());
    }

    public CompletableFuture<Void> doFurtherPeerExchange() {
        return doPeerExchange(peerExchangeStrategy.getAddressesForFurtherPeerExchange());
    }

    private CompletableFuture<Void> doPeerExchange(List<Address> candidates) {
        if (candidates.isEmpty() || isStopped) {
            return CompletableFuture.completedFuture(null);
        }
        log.info("Node {} starts peer exchange with: {}", node,
                StringUtils.truncate(candidates.stream()
                        .map(Address::toString)
                        .collect(Collectors.toList()).toString()));
        List<CompletableFuture<Boolean>> allFutures = candidates.stream()
                .map(this::doPeerExchangeAsync)
                .collect(Collectors.toList());
        return CompletableFutureUtils.allOf(allFutures)
                .thenApply(resultList -> {
                    int numSuccess = (int) resultList.stream().filter(e -> e).count();
                    log.info("Node {} completed peer exchange to {} candidates. {} requests successfully completed.",
                            node, candidates.size(), numSuccess);
                    if (peerExchangeStrategy.redoInitialPeerExchange(numSuccess, candidates.size())) {
                        log.info("Node {} repeats the initial peer exchange after {} sec as it has not reached sufficient connections " +
                                "or received sufficient peers", node, doInitialPeerExchangeDelaySec);
                        scheduler = Optional.of(Scheduler.run(this::doInitialPeerExchange)
                                .after(doInitialPeerExchangeDelaySec, TimeUnit.SECONDS)
                                .name("PeerExchangeService.scheduler-" + node));
                        doInitialPeerExchangeDelaySec = Math.min(60, doInitialPeerExchangeDelaySec * 2);
                    } else {
                        scheduler.ifPresent(Scheduler::stop);
                    }
                    return null;
                });
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
                log.warn("Node {} : requestHandlerMap contains already {}. " +
                        "We dispose the existing handler and start a new one.", node, peerAddress);
                requestHandlerMap.get(key).dispose();
            }

            PeerExchangeRequestHandler handler = new PeerExchangeRequestHandler(node, connection);
            requestHandlerMap.put(key, handler);
            Set<Peer> myPeers = peerExchangeStrategy.getPeers(peerAddress);

            Set<Peer> peers = handler.request(myPeers).join();
            peerExchangeStrategy.addReportedPeers(peers, peerAddress);
            requestHandlerMap.remove(key);
            return true;
        } catch (Throwable throwable) {
            if (key != null) {
                requestHandlerMap.remove(key);
            }
            // Expect ConnectException if peer is not available 
            return false;
        }
    }

    public void shutdown() {
        isStopped = true;
        scheduler.ifPresent(Scheduler::stop);
        requestHandlerMap.values().forEach(PeerExchangeRequestHandler::dispose);
        requestHandlerMap.clear();
        peerExchangeStrategy.shutdown();
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof PeerExchangeRequest request) {
            log.debug("Node {} received PeerExchangeRequest with myPeers {}", node, request.peers());
            Address peerAddress = connection.getPeerAddress();
            peerExchangeStrategy.addReportedPeers(request.peers(), peerAddress);
            Set<Peer> myPeers = peerExchangeStrategy.getPeers(peerAddress);
            NETWORK_IO_POOL.submit(() -> node.send(new PeerExchangeResponse(request.nonce(), myPeers), connection));
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