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

package network.misq.network.p2p.services.router.gossip;

import network.misq.common.util.CollectionUtil;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.PeerGroup;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GossipRouter implements Node.Listener {
    private static final long BROADCAST_TIMEOUT = 90;

    private final Node node;
    private final PeerGroup peerGroup;
    private final Set<Node.Listener> listeners = new CopyOnWriteArraySet<>();

    public GossipRouter(Node node, PeerGroup peerGroup) {
        this.node = node;
        this.peerGroup = peerGroup;

        node.addListener(this);
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof GossipMessage gossipMessage) {
            listeners.forEach(listener -> listener.onMessage(gossipMessage.message(), connection, nodeId));
        }
    }

    public CompletableFuture<GossipResult> broadcast(Message message) {
        long ts = System.currentTimeMillis();
        CompletableFuture<GossipResult> future = new CompletableFuture<>();
        future.orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        AtomicInteger numSuccess = new AtomicInteger(0);
        AtomicInteger numFaults = new AtomicInteger(0);

        Stream<Address> allConnectedPeerAddresses = peerGroup.getAllConnectedPeerAddresses();
        long target = allConnectedPeerAddresses.count();
        allConnectedPeerAddresses.forEach(address -> {
            node.send(new GossipMessage(message), address)
                    .whenComplete((connection, t) -> {
                        if (connection != null) {
                            numSuccess.incrementAndGet();
                        } else {
                            numFaults.incrementAndGet();
                        }
                        if (numSuccess.get() + numFaults.get() == target) {
                            future.complete(new GossipResult(numSuccess.get(),
                                    numFaults.get(),
                                    System.currentTimeMillis() - ts));
                        }
                    });
        });
        
      /*  Set<Address> connectedPeerAddresses = allConnectedPeerAddresses.collect(Collectors.toSet());
        int target = connectedPeerAddresses.size();
        connectedPeerAddresses.forEach(address -> {
            node.send(new GossipMessage(message), address)
                    .whenComplete((connection, t) -> {
                        if (connection != null) {
                            numSuccess.incrementAndGet();
                        } else {
                            numFaults.incrementAndGet();
                        }
                        if (numSuccess.get() + numFaults.get() == target) {
                            future.complete(new GossipResult(numSuccess.get(),
                                    numFaults.get(),
                                    System.currentTimeMillis() - ts));
                        }
                    });
        });*/
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
