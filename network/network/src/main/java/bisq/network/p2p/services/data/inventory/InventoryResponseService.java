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

package bisq.network.p2p.services.data.inventory;

import bisq.common.util.ByteUnit;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class InventoryResponseService implements Node.Listener {
    private final Node node;
    private final Map<InventoryFilterType, FilterService<? extends InventoryFilter>> filterServiceMap;

    InventoryResponseService(Node node, Map<InventoryFilterType, FilterService<? extends InventoryFilter>> filterServiceMap) {
        this.node = node;
        this.filterServiceMap = filterServiceMap;

        node.addListener(this);
    }

    public void shutdown() {
        node.removeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof InventoryRequest) {
            InventoryRequest request = (InventoryRequest) envelopePayloadMessage;
            handleInventoryRequest(request, connection);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    private void handleInventoryRequest(InventoryRequest request, Connection connection) {
        InventoryFilter inventoryFilter = request.getInventoryFilter();
        double size = ByteUnit.BYTE.toKB(inventoryFilter.toProto().getSerializedSize());
        log.info("Received an InventoryRequest from peer {}. Size: {} kb. Filter details: {}",
                connection.getPeerAddress(), size, inventoryFilter.getDetails());

        InventoryFilterType inventoryFilterType = inventoryFilter.getInventoryFilterType();
        if (filterServiceMap.containsKey(inventoryFilterType)) {
            FilterService<? extends InventoryFilter> filterService = filterServiceMap.get(inventoryFilterType);
            Inventory inventory = filterService.createInventory(inventoryFilter);
            NetworkService.NETWORK_IO_POOL.submit(() -> node.send(new InventoryResponse(inventory, request.getNonce()), connection));
        } else {
            log.warn("We got an inventoryRequest with filterType {} which we do not support." +
                            "This should never happen if our feature entries are correct and if the peers code is executed as expected.",
                    inventoryFilterType);
        }
    }
}