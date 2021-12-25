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

package network.misq.network.p2p.services.data;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.Disposable;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.broadcast.BroadcastResult;
import network.misq.network.p2p.services.broadcast.Broadcaster;
import network.misq.network.p2p.services.data.filter.DataFilter;
import network.misq.network.p2p.services.data.inventory.InventoryRequestHandler;
import network.misq.network.p2p.services.data.inventory.InventoryResponseHandler;
import network.misq.network.p2p.services.data.inventory.RequestInventoryResult;
import network.misq.network.p2p.services.data.storage.Storage;
import network.misq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import network.misq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.security.KeyPairRepository;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Preliminary ideas:
 * Use an ephemeral ID (key) for mailbox msg so a receiver can pick the right msg to decode.
 * At initial data request split the request in chunks. E.g. Give me the first 25% of your data to first node,
 * Give me the second 25% of your data to second node, ... with some overlaps as nodes do not have all the same data.
 * The data need to be sorted deterministically.
 * For non-temporary data use age and deliver historical data only on extra demand.
 * At startup give users option to use clear-net for initial sync and switch to tor after that. Might require a restart ;-(.
 * That way the user trades of speed with loss of little privacy (the other nodes learn that that IP uses misq).
 * Probably acceptable trade off for many users. Would be good if the restart could be avoided. Maybe not that hard...
 * <p>
 * UPDATE: Use BloomFilter class from guava lib
 */
@Slf4j
public class DataService implements Node.Listener {
    private static final long BROADCAST_TIMEOUT = 90;

    public static record Config(String baseDirPath) {
    }


    private final Node node;
    @Getter
    private final PeerGroupService peerGroupService;
    private final KeyPairRepository keyPairRepository;
    private final Storage storage;
    private final Broadcaster broadcaster;
    private final Set<DataListener> dataListeners = new CopyOnWriteArraySet<>();
    private final Map<String, InventoryResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, InventoryRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public DataService(Node node, PeerGroupService peerGroupService, KeyPairRepository keyPairRepository, Config config) {
        this.node = node;
        this.peerGroupService = peerGroupService;
        this.keyPairRepository = keyPairRepository;
        this.storage = new Storage(config.baseDirPath());

        broadcaster = new Broadcaster(node, peerGroupService.getPeerGroup());
        broadcaster.addMessageListener(this);

        keyPairRepository.getOrCreateKeyPair(KeyPairRepository.DEFAULT);
        // node.addListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (dataListeners.isEmpty()) {
            return;
        }

        if (message instanceof AddDataRequest addDataRequest) {
            storage.addRequest(addDataRequest)
                    .whenComplete((optionalData, throwable) -> {
                        optionalData.ifPresent(networkData -> {
                            runAsync(() -> dataListeners.forEach(listener -> {
                                listener.onNetworkDataAdded(networkData);
                            }));
                        });
                    });
        } else if (message instanceof RemoveDataRequest removeDataRequest) {
            // Message removedItem = storage.remove(removeDataRequest.getMapKey());
              /*  if (removedItem != null) {
                    dataListeners.forEach(listener -> listener.onDataRemoved(message));
                }*/
        }
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

    public CompletableFuture<BroadcastResult> addMailboxPayload(MailboxPayload mailboxPayload,
                                                                KeyPair senderKeyPair,
                                                                PublicKey receiverPublicKey) {
        return storage.getOrCreateMailboxDataStore(mailboxPayload.getMetaData())
                .thenCompose(store -> {
                    try {
                        AddMailboxRequest addRequest = AddMailboxRequest.from(store,
                                mailboxPayload,
                                senderKeyPair,
                                receiverPublicKey);
                        store.add(addRequest);
                        return broadcaster.broadcast(new AddDataRequest(addRequest));
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public CompletableFuture<BroadcastResult> addNetworkPayload(NetworkPayload networkPayload, KeyPair keyPair) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            return storage.getOrCreateAuthenticatedDataStore(authenticatedPayload.getMetaData())
                    .thenCompose(store -> {
                        try {
                            AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedPayload, keyPair);
                            store.add(addRequest);
                            return broadcaster.broadcast(new AddDataRequest(addRequest));
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                            throw new CompletionException(e);
                        }
                    });
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException(""));
        }

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

    public void addDataListener(DataListener listener) {
        dataListeners.add(listener);
    }

    public void removeDataListener(DataListener listener) {
        dataListeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter, Address address) {
        long ts = System.currentTimeMillis();
        CompletableFuture<RequestInventoryResult> future = new CompletableFuture<>();
        future.orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        node.getConnection(address)
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
        dataListeners.clear();
        //todo
        broadcaster.shutdown();
        storage.shutdown();

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
