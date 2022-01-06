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

import bisq.common.Disposable;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.broadcast.Broadcaster;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.inventory.InventoryRequestHandler;
import bisq.network.p2p.services.data.inventory.InventoryResponseHandler;
import bisq.network.p2p.services.data.inventory.RequestInventoryResult;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.security.KeyPairService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * Preliminary ideas:
 * Use an ephemeral ID (key) for mailbox msg so a receiver can pick the right msg to decode.
 * At initial data request split the request in chunks. E.g. Give me the first 25% of your data to first node,
 * Give me the second 25% of your data to second node, ... with some overlaps as nodes do not have all the same data.
 * The data need to be sorted deterministically.
 * For non-temporary data use age and deliver historical data only on extra demand.
 * At startup give users option to use clear-net for initial sync and switch to tor after that. Might require a restart ;-(.
 * That way the user trades of speed with loss of little privacy (the other nodes learn that that IP uses bisq).
 * Probably acceptable trade off for many users. Would be good if the restart could be avoided. Maybe not that hard...
 * <p>
 * UPDATE: Use BloomFilter class from guava lib
 */
@Slf4j
public class DataServicePerTransport implements Node.Listener {
    private static final long BROADCAST_TIMEOUT = 90;

    private final Node node;
    private final Broadcaster broadcaster;
    private final Map<String, InventoryResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, InventoryRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public DataServicePerTransport(Node node, PeerGroupService peerGroupService, KeyPairService keyPairService) {
        this.node = node;

        broadcaster = new Broadcaster(node, peerGroupService.getPeerGroup());
        node.addListener(this);
    }

    public void addListener(Node.Listener listener) {
        node.addListener(listener);
    }

    public void removeListener(Node.Listener listener) {
        node.removeListener(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
    }

    @Override
    public void onConnection(Connection connection) {
        addResponseHandler(connection);
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = connection.getId();
        if (responseHandlerMap.containsKey(key)) {
            responseHandlerMap.get(key).dispose();
            responseHandlerMap.remove(key);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public CompletableFuture<BroadcastResult> broadcast(AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
        return broadcaster.broadcast(new AddDataRequest(addAuthenticatedDataRequest));
    }

    public CompletableFuture<BroadcastResult> reBroadcast(AddDataRequest addDataRequest) {
        return broadcaster.reBroadcast(addDataRequest);
    }

    public CompletableFuture<BroadcastResult> requestRemoveData(Message message) {
        //   RemoveDataRequest removeDataRequest = new RemoveDataRequest(new MapKey(message));
        //  storage.remove(removeDataRequest.getMapKey());
        // return router.broadcast(removeDataRequest);
        return null;
    }

    public CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter) {
        return requestInventory(dataFilter, broadcaster.getPeerAddressesForInventoryRequest())
                .whenComplete((requestInventoryResult, throwable) -> {
                    if (requestInventoryResult != null) {
                      /*  storage.add(requestInventoryResult.getInventory())
                                .handle((inventory, error) -> {
                                    if (inventory != null) {
                                        return requestInventoryResult;
                                    } else {
                                        return CompletableFuture.failedFuture(error);
                                    }
                                });*/
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter, Address address) {
        long ts = System.currentTimeMillis();
        CompletableFuture<RequestInventoryResult> future = new CompletableFuture<>();
        future.orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        supplyAsync(() -> node.getConnection(address), NetworkService.NETWORK_IO_POOL)
                .thenCompose(connection -> {
                    InventoryRequestHandler requestHandler = new InventoryRequestHandler(node, connection);
                    requestHandlerMap.put(connection.getId(), requestHandler);
                    return requestHandler.request(dataFilter);
                })
                .whenComplete((inventory, throwable) -> {
                    if (inventory != null) {
                        future.complete(new RequestInventoryResult(inventory, System.currentTimeMillis() - ts));
                    } else {
                        future.completeExceptionally(throwable);
                    }
                });
        return future;
    }


    private void addResponseHandler(Connection connection) {
      /*  InventoryResponseHandler responseHandler = new InventoryResponseHandler(node,
                connection,
                storage::getInventory,
                () -> responseHandlerMap.remove(connection.getId()));
        responseHandlerMap.put(connection.getId(), responseHandler);*/
    }

    public CompletableFuture<Void> shutdown() {
        //todo
        broadcaster.shutdown();

        requestHandlerMap.values().forEach(Disposable::dispose);
        requestHandlerMap.clear();

        responseHandlerMap.values().forEach(Disposable::dispose);
        responseHandlerMap.clear();

        return CompletableFuture.completedFuture(null);
    }

    public void disposeAndRemove(String key, Map<String, ? extends Disposable> map) {
        if (map.containsKey(key)) {
            map.get(key).dispose();
            map.remove(key);
        }
    }

    public void disposeAndRemoveAll(Map<String, ? extends Disposable> map) {
        map.values().forEach(Disposable::dispose);
        map.clear();
    }
}
