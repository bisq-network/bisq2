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
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.Result;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyPayload;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
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
public class DataService implements DataNetworkService.Listener {


    public interface Listener {
        void onNetworkPayloadAdded(NetworkPayload networkPayload);

        void onNetworkPayloadRemoved(NetworkPayload networkPayload);
    }

    @Getter
    private final StorageService storageService;
    private final Set<DataService.Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<Transport.Type, DataNetworkService> dataNetworkServices = new ConcurrentHashMap<>();

    public DataService(StorageService storageService) {
        this.storageService = storageService;
    }

    // todo a bit of a hack that way...
    public DataNetworkService getDataServicePerTransport(Transport.Type transportType, Node defaultNode, PeerGroupService peerGroupService) {
        DataNetworkService dataNetworkService = new DataNetworkService(defaultNode, peerGroupService, storageService::getInventoryOfAllStores);
        dataNetworkServices.put(transportType, dataNetworkService);
        dataNetworkService.addListener(this);
        return dataNetworkService;
    }

    public void initialize() {

    }

    public CompletableFuture<Void> shutdown() {
        storageService.shutdown();
        listeners.clear();
        return CompletableFuture.completedFuture(null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataNetworkService.Listeners
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
    public void onStateChanged(PeerGroupService.State state, DataNetworkService dataNetworkService) {
        if (state == PeerGroupService.State.RUNNING) {
            requestInventory(new DataFilter(new HashSet<>(storageService.getFilterEntries(StorageService.StoreType.ALL))), dataNetworkService);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthenticatedPayload> getAllAuthenticatedPayload() {
        return storageService.getAllAuthenticatedPayload();
    }

    public Stream<AuthenticatedPayload> getAuthenticatedPayloadByStoreName(String storeName) {
        return storageService.getAuthenticatedPayloadStream(storeName);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addNetworkPayloadAsync(NetworkPayload networkPayload, KeyPair keyPair) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            return storageService.getOrCreateAuthenticatedDataStore(authenticatedPayload.getMetaData())
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
            return storageService.getOrCreateAppendOnlyDataStore(appendOnlyPayload.getMetaData())
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
        return storageService.getOrCreateMailboxDataStore(mailboxPayload.getMetaData())
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Remove data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> removeNetworkPayloadAsync(NetworkPayload networkPayload, KeyPair keyPair) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            return storageService.getOrCreateAuthenticatedDataStore(authenticatedPayload.getMetaData())
                    .thenApply(store -> {
                        try {
                            RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedPayload, keyPair);
                            Result result = store.remove(request);
                            if (result.isSuccess()) {
                                listeners.forEach(listener -> listener.onNetworkPayloadRemoved(networkPayload));
                                return dataNetworkServices.values().stream()
                                        .map(service -> service.broadcast(request))
                                        .collect(Collectors.toList());
                            } else {
                                return new ArrayList<>();
                            }
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                            throw new CompletionException(e);
                        }
                    });
        } else if (networkPayload instanceof AppendOnlyPayload) {
            throw new IllegalArgumentException("AppendOnlyPayload cannot be removed");
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException(""));
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public void requestInventory() {
        requestInventory(StorageService.StoreType.ALL);
    }

    public void requestInventory(StorageService.StoreType storeType) {
        requestInventory(new DataFilter(new HashSet<>(storageService.getFilterEntries(storeType))));
    }

    public void requestInventory(String storeName) {
        requestInventory(new DataFilter(new HashSet<>(storageService.getFilterEntries(storeName))));
    }


    public void requestInventory(DataFilter dataFilter) {
        dataNetworkServices.values().forEach(service -> requestInventory(dataFilter, service));
    }

    public void requestInventory(DataFilter dataFilter, DataNetworkService dataNetworkService) {
        dataNetworkService.requestInventory(dataFilter).forEach(future -> {
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addListener(DataService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataService.Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAddDataRequest(AddDataRequest addDataRequest, boolean allowReBroadcast) {
        storageService.onAddDataRequest(addDataRequest)
                .whenComplete((optionalData, throwable) -> {
                    optionalData.ifPresent(networkData -> {
                        // We get called on dispatcher thread with onMessage, and we don't switch thread in 
                        // async calls (todo check if in all cases true)
                        listeners.forEach(listener -> listener.onNetworkPayloadAdded(networkData));
                        // runAsync(() -> listeners.forEach(listener -> listener.onNetworkDataAdded(networkData)), NetworkService.DISPATCHER);
                        if (allowReBroadcast) {
                            dataNetworkServices.values().forEach(e -> e.reBroadcast(addDataRequest));
                        }
                    });
                });
    }

    private void processRemoveDataRequest(RemoveDataRequest removeDataRequest, boolean allowReBroadcast) {
        storageService.onRemoveDataRequest(removeDataRequest)
                .whenComplete((optionalData, throwable) -> {
                    optionalData.ifPresent(networkData -> {
                        // We get called on dispatcher thread with onMessage, and we don't switch thread in 
                        // async calls
                        listeners.forEach(listener -> listener.onNetworkPayloadRemoved(networkData));
                        if (allowReBroadcast) {
                            dataNetworkServices.values().forEach(e -> e.reBroadcast(removeDataRequest));
                        }
                    });
                });
    }
}