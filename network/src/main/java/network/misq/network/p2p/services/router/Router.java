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

package network.misq.network.p2p.services.router;

import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.router.gossip.GossipResult;
import network.misq.network.p2p.services.router.gossip.GossipRouter;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Responsibility:
 * - Supports multiple routers
 * - Decides which router is used for which message
 * - MessageListeners will get the consolidated messages from multiple routers
 */
public class Router implements Node.Listener {
    private final GossipRouter gossipRouter;
    private final Set<Node.Listener> listeners = new CopyOnWriteArraySet<>();

    public Router(Node node, PeerGroup peerGroup) {
        gossipRouter = new GossipRouter(node, peerGroup);
        gossipRouter.addMessageListener(this);
    }

    public CompletableFuture<GossipResult> broadcast(Message message) {
        return gossipRouter.broadcast(message);
    }

    public void addMessageListener(Node.Listener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(Node.Listener listener) {
        listeners.remove(listener);
    }


    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        listeners.forEach(listener -> listener.onMessage(message, connection, nodeId));
    }

    public Address getPeerAddressesForInventoryRequest() {
        return gossipRouter.getPeerAddressesForInventoryRequest();
    }

    public void shutdown() {
        listeners.clear();
        gossipRouter.removeMessageListener(this);
        gossipRouter.shutdown();
    }
}
