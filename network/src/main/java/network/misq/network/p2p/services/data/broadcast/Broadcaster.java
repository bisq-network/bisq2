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

package network.misq.network.p2p.services.data.broadcast;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.CollectionUtil;
import network.misq.network.NetworkService;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.PeerGroup;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;

@Slf4j
public class Broadcaster implements Node.Listener {
    private static final long BROADCAST_TIMEOUT = 90;

    private final Node node;
    private final PeerGroup peerGroup;
    private final Set<Node.Listener> listeners = new CopyOnWriteArraySet<>();

    public Broadcaster(Node node, PeerGroup peerGroup) {
        this.node = node;
        this.peerGroup = peerGroup;

        node.addListener(this);
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (listeners.isEmpty()) {
            return;
        }

        if (message instanceof BroadcastMessage broadcastMessage) {
            listeners.forEach(listener -> listener.onMessage(broadcastMessage.message(), connection, nodeId));
        }
    }

    public CompletableFuture<BroadcastResult> reBroadcast(Message message) {
        return CompletableFuture.supplyAsync(() -> broadcast(message, 0.75).join(),
                CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS));
    }

    public CompletableFuture<BroadcastResult> broadcast(Message message) {
        return broadcast(message, 1);
    }

    public CompletableFuture<BroadcastResult> broadcast(Message message, double distributionFactor) {
        long ts = System.currentTimeMillis();
        CompletableFuture<BroadcastResult> future = new CompletableFuture<BroadcastResult>()
                .orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        AtomicInteger numSuccess = new AtomicInteger(0);
        AtomicInteger numFaults = new AtomicInteger(0);
        long numConnections = peerGroup.getAllConnections().count();
        long numBroadcasts = Math.min(numConnections, Math.round(numConnections * distributionFactor));
        log.debug("Broadcast message to {} out of {} peers. distributionFactor={}",
                numBroadcasts, numConnections, distributionFactor);
        List<Connection> allConnections = peerGroup.getAllConnections().collect(Collectors.toList());
        Collections.shuffle(allConnections);
        runAsync(() -> {
            allConnections.stream()
                    .limit(numBroadcasts)
                    .forEach(connection -> {
                        log.debug("Node {} broadcast to {}", node, connection.getPeerAddress());
                        try {
                            node.send(new BroadcastMessage(message), connection);
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
        }, NetworkService.NETWORK_IO_POOL);
        return future;
    }

    public Address getPeerAddressesForInventoryRequest() {
        return CollectionUtil.getRandomElement(peerGroup.getAllConnectedPeerAddresses().collect(Collectors.toSet()));
    }

    public void addMessageListener(Node.Listener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(Node.Listener listener) {
        listeners.remove(listener);
    }

    public void shutdown() {
        listeners.clear();

        node.removeListener(this);
    }
}
