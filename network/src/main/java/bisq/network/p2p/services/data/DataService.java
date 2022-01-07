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

import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.filter.BisqBloomFilter;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.Result;
import bisq.network.p2p.services.data.storage.Storage;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyPayload;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Single instance for data distribution. Uses DataNetworkService instances for broadcast and listening for
 * messages on the supported transport networks as well for the inventory service.
 */
@Slf4j
public class DataService implements Node.Listener {


    public interface Listener {
        void onNetworkPayloadAdded(NetworkPayload networkPayload);

        void onNetworkPayloadRemoved(NetworkPayload networkPayload);
    }

    @Getter
    private final Storage storage;
    private final Set<DataService.Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<Transport.Type, DataNetworkService> dataNetworkServices = new ConcurrentHashMap<>();

    public DataService(Storage storage) {
        this.storage = storage;
    }

    public DataNetworkService getDataServicePerTransport(Transport.Type transportType, Node defaultNode, PeerGroupService peerGroupService) {
        DataNetworkService dataNetworkService = new DataNetworkService(defaultNode, peerGroupService, storage::getInventoryOfAllStores);
        dataNetworkServices.put(transportType, dataNetworkService);
        dataNetworkService.addListener(this);
        return dataNetworkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthenticatedPayload> getAllAuthenticatedPayload() {
        return storage.getAllAuthenticatedPayload();
    }

    public void requestInventory() {
        requestInventory(Storage.StoreType.ALL);
    }

    public void requestInventory(Storage.StoreType storeType) {
        requestInventory(new BisqBloomFilter(storage.getHashes(storeType)));
    }

    public void requestInventory(String storeName) {
        requestInventory(new BisqBloomFilter(storage.getHashes(storeName)));
    }

    public void requestInventory(DataFilter dataFilter) {
        log.error("requestInventory dataFilter={}", dataFilter);
        dataNetworkServices.values().stream()
                .flatMap(service -> service.requestInventory(dataFilter).stream())
                .forEach(future -> {
                    future.whenComplete(((inventory, throwable) -> {
                        inventory.entries().forEach(dataRequest -> {
                            if (dataRequest instanceof AddDataRequest addDataRequest) {
                                processAddDataRequest(addDataRequest, false);
                            } else if (dataRequest instanceof RemoveDataRequest removeDataRequest) {
                                processRemoveDataRequest(removeDataRequest, false);
                            }
                        });
                    }));
                });
    }


    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addNetworkPayloadAsync(NetworkPayload networkPayload, KeyPair keyPair) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            return storage.getOrCreateAuthenticatedDataStore(authenticatedPayload.getMetaData())
                    .thenApply(store -> {
                        try {
                            AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedPayload, keyPair);
                            Result result = store.add(addRequest);
                            if (result.isSuccess()) {
                                listeners.forEach(listener -> listener.onNetworkPayloadAdded(networkPayload));
                                return dataNetworkServices.values().stream()
                                        .map(service -> service.broadcast(addRequest))
                                        .collect(Collectors.toList());
                            } else {
                                return new ArrayList<>();
                            }
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                            throw new CompletionException(e);
                        }
                    });
        } else if (networkPayload instanceof AppendOnlyPayload appendOnlyPayload) {
            return storage.getOrCreateAppendOnlyDataStore(appendOnlyPayload.getMetaData())
                    .thenApply(store -> {
                        AddAppendOnlyDataRequest addAppendOnlyDataRequest = new AddAppendOnlyDataRequest(appendOnlyPayload);
                        Result result = store.add(addAppendOnlyDataRequest);
                        if (result.isSuccess()) {
                            listeners.forEach(listener -> listener.onNetworkPayloadAdded(networkPayload));
                            return dataNetworkServices.values().stream()
                                    .map(service -> service.broadcast(new AddAppendOnlyDataRequest(appendOnlyPayload)))
                                    .collect(Collectors.toList());
                        } else {
                            return new ArrayList<>();
                        }
                    });
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException(""));
        }
    }

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addMailboxPayloadAsync(MailboxPayload mailboxPayload,
                                                                                              KeyPair senderKeyPair,
                                                                                              PublicKey receiverPublicKey) {
        return storage.getOrCreateMailboxDataStore(mailboxPayload.getMetaData())
                .thenApply(store -> {
                    try {
                        AddMailboxRequest addRequest = AddMailboxRequest.from(store, mailboxPayload, senderKeyPair, receiverPublicKey);
                        Result result = store.add(addRequest);
                        if (result.isSuccess()) {
                            return dataNetworkServices.values().stream()
                                    .map(service -> service.broadcast(addRequest))
                                    .collect(Collectors.toList());
                        } else {
                            return new ArrayList<>();
                        }

                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public void removeNetworkPayload(NetworkPayload networkPayload, KeyPair keyPair) {
        //todo
    }

    public CompletableFuture<Void> shutdown() {
        listeners.clear();
        return CompletableFuture.completedFuture(null);
    }

    public void addListener(DataService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataService.Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listeners on  DataServicePerTransports
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof AddDataRequest addDataRequest) {
            processAddDataRequest(addDataRequest, true);
        } else if (message instanceof RemoveDataRequest removeDataRequest) {
            processRemoveDataRequest(removeDataRequest, true);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAddDataRequest(AddDataRequest addDataRequest, boolean allowReBroadcast) {
        storage.onAddDataRequest(addDataRequest)
                .whenComplete((optionalData, throwable) -> {
                    optionalData.ifPresent(networkData -> {
                        // We get called on dispatcher thread with onMessage, and we don't switch thread in 
                        // async calls (todo check if in all cases true)
                        log.error("processAddDataRequest");
                        listeners.forEach(listener -> listener.onNetworkPayloadAdded(networkData));
                        // runAsync(() -> listeners.forEach(listener -> listener.onNetworkDataAdded(networkData)), NetworkService.DISPATCHER);
                        if (allowReBroadcast) {
                            dataNetworkServices.values().forEach(e -> e.reBroadcast(addDataRequest));
                        }
                    });
                });
    }

    private void processRemoveDataRequest(RemoveDataRequest removeDataRequest, boolean allowReBroadcast) {
        //todo
    }
}