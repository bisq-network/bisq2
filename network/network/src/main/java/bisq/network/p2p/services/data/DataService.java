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

import bisq.common.timer.Scheduler;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.Result;
import bisq.network.p2p.services.data.storage.StorageData;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
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
public class DataService implements DataNetworkService.Listener {
    public static class BroadCastDataResult extends HashMap<Transport.Type, CompletableFuture<BroadcastResult>> {
        public BroadCastDataResult(Map<Transport.Type, CompletableFuture<BroadcastResult>> map) {
            super(map);
        }

        public BroadCastDataResult() {
            super();
        }
    }

    public interface Listener {
        default void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        }

        default void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        }

        default void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        }

        default void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        }

        default void onAppendOnlyDataAdded(AppendOnlyData appendOnlyData) {
        }

        default void onMailboxDataAdded(MailboxData mailboxData) {
        }

        default void onMailboxDataRemoved(MailboxData mailboxData) {
        }
    }

    @Getter
    private final StorageService storageService;
    private final Set<DataService.Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<Transport.Type, DataNetworkService> dataNetworkServiceByTransportType = new ConcurrentHashMap<>();

    public DataService(StorageService storageService) {
        this.storageService = storageService;

        storageService.addListener(new StorageService.Listener() {
            @Override
            public void onAdded(StorageData storageData) {
                if (storageData instanceof AuthorizedData) {
                    listeners.forEach(e -> e.onAuthorizedDataAdded((AuthorizedData) storageData));
                } else if (storageData instanceof AuthenticatedData) {
                    listeners.forEach(e -> e.onAuthenticatedDataAdded((AuthenticatedData) storageData));
                } else if (storageData instanceof MailboxData) {
                    listeners.forEach(e -> e.onMailboxDataAdded((MailboxData) storageData));
                } else if (storageData instanceof AppendOnlyData) {
                    listeners.forEach(e -> e.onAppendOnlyDataAdded((AppendOnlyData) storageData));
                }
            }

            @Override
            public void onRemoved(StorageData storageData) {
                if (storageData instanceof AuthorizedData) {
                    listeners.forEach(e -> e.onAuthorizedDataRemoved((AuthorizedData) storageData));
                } else if (storageData instanceof AuthenticatedData) {
                    listeners.forEach(e -> e.onAuthenticatedDataRemoved((AuthenticatedData) storageData));
                } else if (storageData instanceof MailboxData) {
                    listeners.forEach(e -> e.onMailboxDataRemoved((MailboxData) storageData));
                }
            }
        });
    }

    // todo a bit of a hack that way...
    public DataNetworkService getDataServicePerTransport(Transport.Type transportType, Node defaultNode, PeerGroupService peerGroupService) {
        DataNetworkService dataNetworkService = new DataNetworkService(defaultNode, peerGroupService, storageService::getInventoryOfAllStores);
        dataNetworkServiceByTransportType.put(transportType, dataNetworkService);
        dataNetworkService.addListener(this);
        return dataNetworkService;
    }

    public CompletableFuture<Boolean> shutdown() {
        dataNetworkServiceByTransportType.values().forEach(DataNetworkService::shutdown);
        storageService.shutdown();
        listeners.clear();
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataNetworkService.Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
        if (networkMessage instanceof AddDataRequest) {
            processAddDataRequest((AddDataRequest) networkMessage, true);
        } else if (networkMessage instanceof RemoveDataRequest) {
            processRemoveDataRequest((RemoveDataRequest) networkMessage, true);
        }
    }

    @Override
    public void onStateChanged(PeerGroupService.State state, DataNetworkService dataNetworkService) {
        if (state == PeerGroupService.State.RUNNING) {
            log.info("PeerGroupService initialized. We start the inventory request after a short delay.");
            Scheduler.run(() -> doRequestInventory(dataNetworkService)).after(500);
        }
    }

    @Override
    public void onSufficientlyConnected(int numConnections, DataNetworkService dataNetworkService) {
        log.info("We are sufficiently connected to start the inventory request. numConnections={}", numConnections);
        doRequestInventory(dataNetworkService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthenticatedData> getAllAuthenticatedData() {
        return storageService.getAllAuthenticatedPayload();
    }

    public Stream<AuthorizedData> getAllAuthorizedData() {
        return getAllAuthenticatedData()
                .filter(authenticatedData -> authenticatedData instanceof AuthorizedData)
                .map(authenticatedData -> (AuthorizedData) authenticatedData);
    }

    public Stream<AuthenticatedData> getAuthenticatedPayloadStreamByStoreName(String storeName) {
        return storageService.getAuthenticatedPayloadStream(storeName);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadCastDataResult> addAuthenticatedData(AuthenticatedData authenticatedData, KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getMetaData())
                .thenApply(store -> {
                    try {
                        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        Result result = store.add(request);
                        if (result.isSuccess()) {
                            if (authenticatedData instanceof AuthorizedData) {
                                listeners.forEach(e -> e.onAuthorizedDataAdded((AuthorizedData) authenticatedData));
                            } else {
                                listeners.forEach(e -> e.onAuthenticatedDataAdded(authenticatedData));
                            }
                            return new BroadCastDataResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadCastDataResult();
                        }
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public CompletableFuture<BroadCastDataResult> addAppendOnlyData(AppendOnlyData appendOnlyData) {
        return storageService.getOrCreateAppendOnlyDataStore(appendOnlyData.getMetaData())
                .thenApply(store -> {
                    AddAppendOnlyDataRequest request = new AddAppendOnlyDataRequest(appendOnlyData);
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        listeners.forEach(listener -> listener.onAppendOnlyDataAdded(appendOnlyData));
                        return new BroadCastDataResult(dataNetworkServiceByTransportType.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                    } else {
                        return new BroadCastDataResult();
                    }
                });
    }

    public CompletableFuture<BroadCastDataResult> addMailboxData(MailboxData mailboxData,
                                                                 KeyPair senderKeyPair,
                                                                 PublicKey receiverPublicKey) {
        return storageService.getOrCreateMailboxDataStore(mailboxData.getMetaData())
                .thenApply(store -> {
                    try {
                        AddMailboxRequest request = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverPublicKey);
                        Result result = store.add(request);
                        if (result.isSuccess()) {
                            listeners.forEach(listener -> listener.onMailboxDataAdded(mailboxData));
                            return new BroadCastDataResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadCastDataResult();
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

    public CompletableFuture<BroadCastDataResult> removeAuthenticatedData(AuthenticatedData authenticatedData,
                                                                          KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getMetaData())
                .thenApply(store -> {
                    try {
                        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        Result result = store.remove(request);
                        if (result.isSuccess()) {
                            if (authenticatedData instanceof AuthorizedData) {
                                listeners.forEach(e -> e.onAuthorizedDataRemoved((AuthorizedData) authenticatedData));
                            } else {
                                listeners.forEach(e -> e.onAuthenticatedDataRemoved(authenticatedData));
                            }
                            return new BroadCastDataResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadCastDataResult();
                        }
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public CompletableFuture<BroadCastDataResult> removeMailboxData(MailboxData mailboxData, KeyPair keyPair) {
        return storageService.getOrCreateMailboxDataStore(mailboxData.getMetaData())
                .thenApply(store -> {
                    try {
                        RemoveMailboxRequest request = RemoveMailboxRequest.from(mailboxData, keyPair);
                        Result result = store.remove(request);
                        if (result.isSuccess()) {
                            listeners.forEach(listener -> listener.onMailboxDataRemoved(mailboxData));
                            return new BroadCastDataResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadCastDataResult();
                        }
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void requestInventory() {
        requestInventory(StorageService.StoreType.ALL);
    }

    public void requestInventory(StorageService.StoreType storeType) {
        requestInventory(new DataFilter(new ArrayList<>(storageService.getFilterEntries(storeType))));
    }

    public void requestInventory(String storeName) {
        requestInventory(new DataFilter(new ArrayList<>(storageService.getFilterEntries(storeName))));
    }

    public void requestInventory(DataFilter dataFilter) {
        dataNetworkServiceByTransportType.values().forEach(service -> requestInventory(dataFilter, service));
    }

    public void requestInventory(DataFilter dataFilter, DataNetworkService dataNetworkService) {
        dataNetworkService.requestInventory(dataFilter).forEach(future -> {
            future.whenComplete(((inventory, throwable) -> {
                inventory.getEntries().forEach(dataRequest -> {
                    if (dataRequest instanceof AddDataRequest) {
                        processAddDataRequest((AddDataRequest) dataRequest, false);
                    } else if (dataRequest instanceof RemoveDataRequest) {
                        processRemoveDataRequest((RemoveDataRequest) dataRequest, false);
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
                    optionalData.ifPresent(storageData -> {
                        // We get called on dispatcher thread with onMessage, and we don't switch thread in 
                        // async calls

                        if (storageData instanceof AuthorizedData) {
                            listeners.forEach(e -> e.onAuthorizedDataAdded((AuthorizedData) storageData));
                        } else if (storageData instanceof AuthenticatedData) {
                            listeners.forEach(e -> e.onAuthenticatedDataAdded((AuthenticatedData) storageData));
                        } else if (storageData instanceof MailboxData) {
                            listeners.forEach(listener -> listener.onMailboxDataAdded((MailboxData) storageData));
                        } else if (storageData instanceof AppendOnlyData) {
                            listeners.forEach(listener -> listener.onAppendOnlyDataAdded((AppendOnlyData) storageData));
                        }
                        if (allowReBroadcast) {
                            dataNetworkServiceByTransportType.values().forEach(e -> e.reBroadcast(addDataRequest));
                        }
                    });
                });
    }

    private void processRemoveDataRequest(RemoveDataRequest removeDataRequest, boolean allowReBroadcast) {
        storageService.onRemoveDataRequest(removeDataRequest)
                .whenComplete((optionalData, throwable) -> {
                    optionalData.ifPresent(storageData -> {
                        // We get called on dispatcher thread with onMessage, and we don't switch thread in 
                        // async calls
                        if (storageData instanceof AuthorizedData) {
                            listeners.forEach(e -> e.onAuthorizedDataRemoved((AuthorizedData) storageData));
                        } else if (storageData instanceof AuthenticatedData) {
                            listeners.forEach(e -> e.onAuthenticatedDataRemoved((AuthenticatedData) storageData));
                        }
                        if (allowReBroadcast) {
                            dataNetworkServiceByTransportType.values().forEach(e -> e.reBroadcast(removeDataRequest));
                        }
                    });
                });
    }

    private void doRequestInventory(DataNetworkService dataNetworkService) {
        requestInventory(new DataFilter(new ArrayList<>(storageService.getFilterEntries(StorageService.StoreType.ALL))), dataNetworkService);
    }
}