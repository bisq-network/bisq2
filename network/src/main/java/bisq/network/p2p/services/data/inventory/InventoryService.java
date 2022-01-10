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

import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.peergroup.PeerGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class InventoryService implements Node.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);


    private final Node node;
    private final PeerGroup peerGroup;
    private final Map<String, InventoryHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final Function<DataFilter, Inventory> inventoryProvider;

    public InventoryService(Node node, PeerGroup peerGroup, Function<DataFilter, Inventory> inventoryProvider) {
        this.node = node;
        this.peerGroup = peerGroup;
        this.inventoryProvider = inventoryProvider;
        this.node.addListener(this);
    }

    public void initialize() {
    }

    public List<CompletableFuture<Inventory>> request(DataFilter dataFilter) {
        int maxRequests = 400;
        return peerGroup.getAllConnections()
                .filter(connection -> !requestHandlerMap.containsKey(connection.getId()))
                .limit(maxRequests)
                .map(connection -> {
                    String key = connection.getId();
                    InventoryHandler handler = new InventoryHandler(node, connection);
                    requestHandlerMap.put(key, handler);
                    return handler.request(dataFilter)
                            .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                            .whenComplete((__, throwable) -> requestHandlerMap.remove(key));
                })
                .collect(Collectors.toList());
    }

    public void shutdown() {
        requestHandlerMap.values().forEach(InventoryHandler::dispose);
        requestHandlerMap.clear();
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof InventoryRequest request) {
            log.debug("Node {} received GetInventoryRequest with nonce {} from {}", node, request.nonce(), connection.getPeerAddress());
            Inventory inventory = inventoryProvider.apply(request.dataFilter());
            NetworkService.NETWORK_IO_POOL.submit(() -> node.send(new InventoryResponse(inventory, request.nonce()), connection));
            log.debug("Node {} sent GetInventoryResponse with inventory {} and nonce {} to {}. Connection={}",
                    node, inventory, request.nonce(), connection.getPeerAddress(), connection.getId());
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
        }
    }
}