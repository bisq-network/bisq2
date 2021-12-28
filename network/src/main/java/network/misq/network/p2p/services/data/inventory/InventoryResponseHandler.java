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

package network.misq.network.p2p.services.data.inventory;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.Disposable;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.data.filter.DataFilter;

import java.util.function.Function;

@Slf4j
public class InventoryResponseHandler implements Node.Listener, Disposable {
    private final Node node;
    private final Connection connection;
    private final Function<DataFilter, Inventory> inventoryProvider;
    private final Runnable completeHandler;

    public InventoryResponseHandler(Node node,
                                    Connection connection,
                                    Function<DataFilter, Inventory> inventoryProvider,
                                    Runnable completeHandler) {
        this.node = node;
        this.connection = connection;
        this.inventoryProvider = inventoryProvider;
        this.completeHandler = completeHandler;

        node.addListener(this);
    }

    public void dispose() {
        node.removeListener(this);
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (this.connection.getId().equals(connection.getId()) &&
                message instanceof InventoryRequest request) {
            Inventory inventory = inventoryProvider.apply(request.getDataFilter());
            node.sendAsync(new InventoryResponse(inventory), connection);
            node.removeListener(this);
            completeHandler.run();
        }
    }

}
