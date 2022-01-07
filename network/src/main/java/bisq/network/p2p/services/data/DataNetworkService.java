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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Responsible for broadcast and inventory service. One instance per transport type.
 */
@Slf4j
public class DataNetworkService {
    private final Node node;
    private final Broadcaster broadcaster;
    private final InventoryService inventoryService;

    public DataNetworkService(Node node,
                              PeerGroupService peerGroupService,
                              Function<DataFilter, Inventory> inventoryProvider) {
        this.node = node;

        PeerGroup peerGroup = peerGroupService.getPeerGroup();
        broadcaster = new Broadcaster(node, peerGroup);
        inventoryService = new InventoryService(node, peerGroup, inventoryProvider);
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

    public void addListener(Node.Listener listener) {
        node.addListener(listener);
    }

    public void removeListener(Node.Listener listener) {
        node.removeListener(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public CompletableFuture<Void> shutdown() {
        broadcaster.shutdown();
        return CompletableFuture.completedFuture(null);
    }
}
