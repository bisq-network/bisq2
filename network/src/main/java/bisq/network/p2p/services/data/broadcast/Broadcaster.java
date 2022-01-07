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

package bisq.network.p2p.services.data.broadcast;

import bisq.common.util.CollectionUtil;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peergroup.PeerGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class Broadcaster {
    private static final long BROADCAST_TIMEOUT = 90;
    private static final long RE_BROADCAST_DELAY_MS = 100;

    private final Node node;
    private final PeerGroup peerGroup;
    private final Set<Node.Listener> listeners = new CopyOnWriteArraySet<>();

    public Broadcaster(Node node, PeerGroup peerGroup) {
        this.node = node;
        this.peerGroup = peerGroup;

    }

    public CompletableFuture<BroadcastResult> reBroadcast(BroadcastMessage broadcastMessage) {
        return CompletableFuture.supplyAsync(() -> broadcast(broadcastMessage, 0.75).join(),
                CompletableFuture.delayedExecutor(RE_BROADCAST_DELAY_MS, TimeUnit.MILLISECONDS));
    }

    public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
        return broadcast(broadcastMessage, 1);
    }

    public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage, double distributionFactor) {
        long ts = System.currentTimeMillis();
        CompletableFuture<BroadcastResult> future = new CompletableFuture<BroadcastResult>()
                .orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        AtomicInteger numSuccess = new AtomicInteger(0);
        AtomicInteger numFaults = new AtomicInteger(0);
        long numConnections = peerGroup.getAllConnections().count();
        long numBroadcasts = Math.min(numConnections, Math.round(numConnections * distributionFactor));
        log.debug("Broadcast proto to {} out of {} peers. distributionFactor={}",
                numBroadcasts, numConnections, distributionFactor);
        List<Connection> allConnections = peerGroup.getAllConnections().collect(Collectors.toList());
        Collections.shuffle(allConnections);
        NetworkService.NETWORK_IO_POOL.submit(() -> {
            allConnections.stream()
                    .limit(numBroadcasts)
                    .forEach(connection -> {
                        log.debug("Node {} broadcast to {}", node, connection.getPeerAddress());
                        try {
                            node.send(broadcastMessage, connection);
                            numSuccess.incrementAndGet();
                        } catch (Throwable throwable) {
                            numFaults.incrementAndGet();
                        }
                        if (numSuccess.get() + numFaults.get() == numBroadcasts) {
                            future.complete(new BroadcastResult(numSuccess.get(),
                                    numFaults.get(),
                                    System.currentTimeMillis() - ts));
                        }
                    });
        });
        return future;
    }

    public Address getPeerAddressesForInventoryRequest() {
        return CollectionUtil.getRandomElement(peerGroup.getAllConnectedPeerAddresses().collect(Collectors.toSet()));
    }

    public void shutdown() {
        listeners.clear();
    }
}
