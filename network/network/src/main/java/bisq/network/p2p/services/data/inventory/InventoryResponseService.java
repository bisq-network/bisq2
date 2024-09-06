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

import bisq.common.data.ByteUnit;
import bisq.common.threading.ThreadName;
import bisq.common.util.ExceptionUtil;
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
import java.util.function.Predicate;

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
        if (envelopePayloadMessage instanceof InventoryRequest request) {
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
        double size = ByteUnit.BYTE.toKB(inventoryFilter.getSerializedSize());
        log.info("Received an InventoryRequest from peer {}. Size: {} kb. Filter details: {}",
                connection.getPeerAddress(), size, inventoryFilter.getDetails());

        InventoryFilterType inventoryFilterType = inventoryFilter.getInventoryFilterType();
        if (filterServiceMap.containsKey(inventoryFilterType)) {
            FilterService<? extends InventoryFilter> filterService = filterServiceMap.get(inventoryFilterType);
            long ts = System.currentTimeMillis();
            int requestersVersion = request.getVersion();

            // We filter out version 1 objects in Add/Remove DataRequest objects which would break the hash when requested from old nodes (pre v.2.1.0)
            // This code can be removed once there are no old nodes expected in the network anymore.
            Predicate<Integer> predicate = distributedDataVersion -> requestersVersion > 0 || distributedDataVersion == 0;

            Inventory inventory = filterService.createInventory(inventoryFilter, predicate);

            // The requestersVersion param can be removed once there are no old nodes expected in the network anymore.
            InventoryResponse inventoryResponse = new InventoryResponse(requestersVersion, inventory, request.getNonce());

            NetworkService.NETWORK_IO_POOL.submit(() -> {
                ThreadName.set(this, "response");
                try {
                    node.send(inventoryResponse, connection);
                    log.info("Successfully sent an InventoryResponse to peer {} with {} kb. Took {} ms",
                            connection.getPeerAddress(),
                            ByteUnit.BYTE.toKB(inventory.getSerializedSize()),
                            System.currentTimeMillis() - ts);
                } catch (Exception e) {
                    log.warn("Error at sending InventoryResponse to {}. {}", connection.getPeerAddress(),
                            ExceptionUtil.getRootCauseMessage(e));
                }
            });
        } else {
            log.warn("We got an inventoryRequest with filterType {} which we do not support." +
                            "This should never happen if our feature entries are correct and if the peers code is executed as expected.",
                    inventoryFilterType);
        }
    }
}