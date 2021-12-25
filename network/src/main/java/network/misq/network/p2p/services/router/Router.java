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
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.router.gossip.GossipRouter;

import java.util.concurrent.CompletableFuture;

/**
 * Responsibility:
 * - Supports multiple routers
 * - Decides which router is used for which message
 * - MessageListeners will get the consolidated messages from multiple routers
 */
public class Router {
    private final GossipRouter gossipRouter;

    public Router(Node node, PeerGroup peerGroup) {
        gossipRouter = new GossipRouter(node, peerGroup);
    }

    public CompletableFuture<BroadcastResult> broadcast(Message message) {
        return gossipRouter.broadcast(message);
    }

    public void addMessageListener(Node.Listener listener) {
        gossipRouter.addMessageListener(listener);
    }

    public void removeMessageListener(Node.Listener listener) {
        gossipRouter.removeMessageListener(listener);
    }

    public Address getPeerAddressesForInventoryRequest() {
        return gossipRouter.getPeerAddressesForInventoryRequest();
    }

    public void shutdown() {
        gossipRouter.shutdown();
    }
}
