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
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.DataStorageResult;
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
import bisq.network.p2p.services.peergroup.PeerGroupManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
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
    private final Map<TransportType, DataNetworkService> dataNetworkServiceByTransportType = new ConcurrentHashMap<>();

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
    public DataNetworkService getDataServicePerTransport(TransportType transportType, Node defaultNode, PeerGroupManager peerGroupManager) {
        DataNetworkService dataNetworkService = new DataNetworkService(defaultNode, peerGroupManager, storageService::getInventoryOfAllStores);
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
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof AddDataRequest) {
            processAddDataRequest((AddDataRequest) envelopePayloadMessage, true);
        } else if (envelopePayloadMessage instanceof RemoveDataRequest) {
            processRemoveDataRequest((RemoveDataRequest) envelopePayloadMessage, true);
        }
    }

    @Override
    public void onStateChanged(PeerGroupManager.State state, DataNetworkService dataNetworkService) {
        if (state == PeerGroupManager.State.RUNNING) {
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

    public Stream<AuthenticatedData> getAuthenticatedData() {
        return storageService.getAuthenticatedData();
    }

    public Stream<AuthorizedData> getAuthorizedData() {
        return getAuthenticatedData()
                .filter(authenticatedData -> authenticatedData instanceof AuthorizedData)
                .map(authenticatedData -> (AuthorizedData) authenticatedData);
    }

    public Stream<AuthenticatedData> getAuthenticatedPayloadStreamByStoreName(String storeName) {
        return storageService.getAuthenticatedData(storeName);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> addAuthenticatedData(AuthenticatedData authenticatedData, KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    try {
                        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        DataStorageResult dataStorageResult = store.add(request);
                        if (dataStorageResult.isSuccess()) {
                            if (authenticatedData instanceof AuthorizedData) {
                                listeners.forEach(e -> e.onAuthorizedDataAdded((AuthorizedData) authenticatedData));
                            } else {
                                listeners.forEach(e -> e.onAuthenticatedDataAdded(authenticatedData));
                            }
                            return new BroadcastResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadcastResult();
                        }
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public CompletableFuture<BroadcastResult> addAuthorizedData(AuthorizedData authorizedData, KeyPair keyPair) {
        return addAuthenticatedData(authorizedData, keyPair);
    }

    public CompletableFuture<BroadcastResult> addAppendOnlyData(AppendOnlyData appendOnlyData) {
        return storageService.getOrCreateAppendOnlyDataStore(appendOnlyData.getMetaData().getClassName())
                .thenApply(store -> {
                    AddAppendOnlyDataRequest request = new AddAppendOnlyDataRequest(appendOnlyData);
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSuccess()) {
                        listeners.forEach(listener -> listener.onAppendOnlyDataAdded(appendOnlyData));
                        return new BroadcastResult(dataNetworkServiceByTransportType.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                    } else {
                        return new BroadcastResult();
                    }
                });
    }

    public CompletableFuture<BroadcastResult> addMailboxData(MailboxData mailboxData,
                                                             KeyPair senderKeyPair,
                                                             PublicKey receiverPublicKey) {
        return storageService.getOrCreateMailboxDataStore(mailboxData.getClassName())
                .thenApply(store -> {
                    try {
                        AddMailboxRequest request = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverPublicKey);
                        DataStorageResult dataStorageResult = store.add(request);
                        if (dataStorageResult.isSuccess()) {
                            listeners.forEach(listener -> listener.onMailboxDataAdded(mailboxData));
                            return new BroadcastResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadcastResult();
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

    public CompletableFuture<BroadcastResult> removeAuthenticatedData(AuthenticatedData authenticatedData, KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    try {
                        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        DataStorageResult dataStorageResult = store.remove(request);
                        if (dataStorageResult.isSuccess()) {
                            if (authenticatedData instanceof AuthorizedData) {
                                listeners.forEach(e -> e.onAuthorizedDataRemoved((AuthorizedData) authenticatedData));
                            } else {
                                listeners.forEach(e -> e.onAuthenticatedDataRemoved(authenticatedData));
                            }
                            return new BroadcastResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadcastResult();
                        }
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public CompletableFuture<BroadcastResult> removeAuthorizedData(AuthorizedData authorizedData, KeyPair keyPair) {
        return removeAuthenticatedData(authorizedData, keyPair);
    }

    public CompletableFuture<BroadcastResult> removeMailboxData(MailboxData mailboxData, KeyPair keyPair) {
        return storageService.getOrCreateMailboxDataStore(mailboxData.getClassName())
                .thenApply(store -> {
                    try {
                        RemoveMailboxRequest request = RemoveMailboxRequest.from(mailboxData, keyPair);
                        DataStorageResult dataStorageResult = store.remove(request);
                        if (dataStorageResult.isSuccess()) {
                            listeners.forEach(listener -> listener.onMailboxDataRemoved(mailboxData));
                            return new BroadcastResult(dataNetworkServiceByTransportType.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().broadcast(request))));
                        } else {
                            return new BroadcastResult();
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