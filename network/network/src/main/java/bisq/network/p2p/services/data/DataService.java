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

import bisq.network.p2p.services.data.broadcast.Broadcaster;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.StorageData;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * Single instance for data distribution. Other transport specific services like DataNetworkService or
 * InventoryService provide data and messages and add the Broadcasters.
 */
@Slf4j
public class DataService implements StorageService.Listener {
    public interface Listener {
        default void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        }

        default void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        }

        default void onAuthorizedDataRefreshed(AuthenticatedData authenticatedData) {
        }

        default void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        }

        default void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        }

        default void onAuthenticatedDataRefreshed(AuthenticatedData authenticatedData) {
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
    private final Set<Broadcaster> broadcasters = new CopyOnWriteArraySet<>();

    public DataService(PersistenceService persistenceService) {
        this.storageService = new StorageService(persistenceService);
        storageService.addListener(this);
    }

    public void shutdown() {
        storageService.removeListener(this);
        listeners.clear();
        broadcasters.clear();
        storageService.shutdown();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //  StorageService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAdded(StorageData storageData) {
        if (storageData instanceof AuthorizedData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAuthorizedDataAdded((AuthorizedData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAuthorizedDataAdded at listener {} failed", listener, e);
                }
            });
        } else if (storageData instanceof AuthenticatedData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAuthenticatedDataAdded((AuthenticatedData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAuthenticatedDataAdded at listener {} failed", listener, e);
                }
            });
        } else if (storageData instanceof MailboxData) {
            listeners.forEach(listener -> {
                try {
                    listener.onMailboxDataAdded((MailboxData) storageData);
                } catch (Exception e) {
                    log.error("Calling onMailboxDataAdded at listener {} failed", listener, e);
                }
            });
        } else if (storageData instanceof AppendOnlyData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAppendOnlyDataAdded((AppendOnlyData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAppendOnlyDataAdded at listener {} failed", listener, e);
                }
            });
        }
    }

    @Override
    public void onRemoved(StorageData storageData) {
        if (storageData instanceof AuthorizedData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAuthorizedDataRemoved((AuthorizedData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAuthorizedDataRemoved at listener {} failed", listener, e);
                }
            });
        } else if (storageData instanceof AuthenticatedData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAuthenticatedDataRemoved((AuthenticatedData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAuthenticatedDataRemoved at listener {} failed", listener, e);
                }
            });
        } else if (storageData instanceof MailboxData) {
            listeners.forEach(listener -> {
                try {
                    listener.onMailboxDataRemoved((MailboxData) storageData);
                } catch (Exception e) {
                    log.error("Calling onMailboxDataRemoved at listener {} failed", listener, e);
                }
            });
        }
    }

    @Override
    public void onRefreshed(StorageData storageData) {
        if (storageData instanceof AuthorizedData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAuthorizedDataRefreshed((AuthorizedData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAuthorizedDataRefreshed at listener {} failed", listener, e);
                }
            });
        } else if (storageData instanceof AuthenticatedData) {
            listeners.forEach(listener -> {
                try {
                    listener.onAuthenticatedDataRefreshed((AuthenticatedData) storageData);
                } catch (Exception e) {
                    log.error("Calling onAuthenticatedDataRefreshed at listener {} failed", listener, e);
                }
            });
        }
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

    public Stream<MailboxData> getMailboxData() {
        return storageService.getMailboxData();
    }

    public Stream<AuthenticatedData> getAuthenticatedPayloadStreamByStoreName(String storeName) {
        return storageService.getAuthenticatedData(storeName);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> addAuthenticatedData(AuthenticatedData authenticatedData,
                                                                   KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    try {
                        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        DataStorageResult dataStorageResult = store.add(request);
                        if (dataStorageResult.isSuccess()) {
                            return new BroadcastResult(broadcasters.stream().map(broadcaster -> broadcaster.broadcast(request)));
                        } else {
                            if (dataStorageResult.isSevereFailure()) {
                                log.warn("addAuthenticatedData failed with severe error. Result={}", dataStorageResult);
                            }
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
                        return new BroadcastResult(broadcasters.stream().map(broadcaster -> broadcaster.broadcast(request)));
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
                            return new BroadcastResult(broadcasters.stream().map(broadcaster -> broadcaster.broadcast(request)));
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
    // Refresh data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> refreshAuthenticatedData(AuthenticatedData authenticatedData,
                                                                       KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    try {
                        RefreshAuthenticatedDataRequest request = RefreshAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        DataStorageResult dataStorageResult = store.refresh(request);
                        if (dataStorageResult.isSuccess()) {
                            return new BroadcastResult(broadcasters.stream().map(broadcaster -> broadcaster.broadcast(request)));
                        } else {
                            if (dataStorageResult.isSevereFailure()) {
                                log.warn("refreshAuthenticatedData failed with severe error. Result={}", dataStorageResult);
                            }
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

    public CompletableFuture<BroadcastResult> removeAuthenticatedData(AuthenticatedData authenticatedData,
                                                                      KeyPair keyPair) {
        return storageService.getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    try {
                        RemoveAuthenticatedDataRequest request = RemoveAuthenticatedDataRequest.from(store, authenticatedData, keyPair);
                        DataStorageResult dataStorageResult = store.remove(request);

                        // Send also with version 0 for backward compatibility
                        RemoveAuthenticatedDataRequest oldVersion = RemoveAuthenticatedDataRequest.cloneWithVersion0(request);
                        DataStorageResult oldVersionDataStorageResult = store.remove(oldVersion);
                        if (dataStorageResult.isSuccess() || oldVersionDataStorageResult.isSuccess()) {
                            broadcasters.forEach(broadcaster -> broadcaster.broadcast(oldVersion));
                        }

                        if (dataStorageResult.isSuccess()) {
                            return new BroadcastResult(broadcasters.stream().map(broadcaster -> broadcaster.broadcast(request)));
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

                        // Send also with version 0 for backward compatibility
                        RemoveMailboxRequest oldVersion = RemoveMailboxRequest.cloneWithVersion0(request);
                        DataStorageResult oldVersionDataStorageResult = store.remove(oldVersion);
                        if (dataStorageResult.isSuccess() || oldVersionDataStorageResult.isSuccess()) {
                            broadcasters.forEach(broadcaster -> broadcaster.broadcast(oldVersion));
                        }

                        if (dataStorageResult.isSuccess()) {
                            return new BroadcastResult(broadcasters.stream().map(broadcaster -> broadcaster.broadcast(request)));
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
    // Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addListener(DataService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataService.Listener listener) {
        listeners.remove(listener);
    }

    public void addBroadcaster(Broadcaster broadcaster) {
        broadcasters.add(broadcaster);
    }

    public void removeBroadcaster(Broadcaster broadcaster) {
        broadcasters.remove(broadcaster);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void processAddDataRequest(AddDataRequest addDataRequest, boolean allowReBroadcast) {
        storageService.onAddDataRequest(addDataRequest)
                .whenComplete((optionalData, throwable) -> optionalData.ifPresent(storageData -> {
                    if (allowReBroadcast) {
                        broadcasters.forEach(e -> e.reBroadcast(addDataRequest));
                    }
                }));
    }

    public void processRemoveDataRequest(RemoveDataRequest removeDataRequest, boolean allowReBroadcast) {
        storageService.onRemoveDataRequest(removeDataRequest)
                .whenComplete((optionalData, throwable) -> optionalData.ifPresent(storageData -> {
                    if (allowReBroadcast) {
                        broadcasters.forEach(e -> e.reBroadcast(removeDataRequest));
                    }
                }));
    }
}