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

package bisq.network.p2p.services.data;

import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.broadcast.Broadcaster;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
 * Responsible for broadcast and inventory service. One instance per transport type.
 */
@Slf4j
public class DataNetworkService implements PeerGroupService.Listener, Node.Listener {

    private final PeerGroup peerGroup;
    private final PeerGroupService peerGroupService;

    public interface Listener {
        void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId);

        void onStateChanged(PeerGroupService.State state, DataNetworkService dataNetworkService);

        void onSufficientlyConnected(int numConnections, DataNetworkService dataNetworkService);
    }

    private final Node node;
    private final Broadcaster broadcaster;
    private final InventoryService inventoryService;
    private final Set<DataNetworkService.Listener> listeners = new CopyOnWriteArraySet<>();

    public DataNetworkService(Node node,
                              PeerGroupService peerGroupService,
                              Function<DataFilter, Inventory> inventoryProvider) {
        this.node = node;
        peerGroup = peerGroupService.getPeerGroup();
        this.peerGroupService = peerGroupService;
        peerGroupService.addListener(this);
        broadcaster = new Broadcaster(node, peerGroup);
        inventoryService = new InventoryService(node, peerGroup, inventoryProvider);
        node.addListener(this);
    }

    public CompletableFuture<Void> shutdown() {
        node.removeListener(this);
        peerGroupService.removeListener(this);
        broadcaster.shutdown();
        return CompletableFuture.completedFuture(null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PeerGroupService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onStateChanged(PeerGroupService.State state) {
        listeners.forEach(listener -> listener.onStateChanged(state, this));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
        listeners.forEach(listener -> listener.onMessage(networkMessage, connection, nodeId));
    }

    @Override
    public void onConnection(Connection connection) {
        if (peerGroup.getNumConnections() > peerGroup.getTargetNumConnectedPeers() / 2) {
            listeners.forEach(listener -> listener.onSufficientlyConnected(peerGroup.getNumConnections(), this));
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
        return broadcaster.broadcast(broadcastMessage);
    }

    CompletableFuture<BroadcastResult> reBroadcast(BroadcastMessage broadcastMessage) {
        return broadcaster.reBroadcast(broadcastMessage);
    }

    List<CompletableFuture<Inventory>> requestInventory(DataFilter dataFilter) {
        return inventoryService.request(dataFilter);
    }

    void addListener(DataNetworkService.Listener listener) {
        listeners.add(listener);
    }

    void removeListener(DataNetworkService.Listener listener) {
        listeners.remove(listener);
    }

}
