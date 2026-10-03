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

package bisq.network.p2p.services.data.storage;


import bisq.common.data.ByteArray;
import bisq.common.timer.Clock;
import bisq.common.timer.SystemClock;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.append.AppendOnlyDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStorageService;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.StoreType.ALL;
import static bisq.network.p2p.services.data.storage.StoreType.APPEND_ONLY_DATA_STORE;
import static bisq.network.p2p.services.data.storage.StoreType.AUTHENTICATED_DATA_STORE;
import static bisq.network.p2p.services.data.storage.StoreType.MAILBOX_DATA_STORE;

@Slf4j
public class StorageService {
    // TODO Use a field for a single handler as only one listener is used by DataService
    public interface Listener {
        void onAdded(StorageData storageData);

        void onRemoved(StorageData storageData);

        void onRefreshed(StorageData storageData);
    }

    final Map<String, AuthenticatedDataStorageService> authenticatedDataStores = new ConcurrentHashMap<>();
    final Map<String, MailboxDataStorageService> mailboxStores = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyDataStorageService> appendOnlyDataStores = new ConcurrentHashMap<>();

    // TODO Use a field for a single handler as only one listener is used by DataService
    private final Map<String, AuthenticatedDataStorageService.Listener> authenticatedDataStoresListeners = new HashMap<>();
    private final Map<String, MailboxDataStorageService.Listener> mailboxStoresListeners = new HashMap<>();
    private final Map<String, AppendOnlyDataStorageService.Listener> appendOnlyDataStoresListeners = new HashMap<>();

    private final PersistenceService persistenceService;
    // The time of the node, for the stores (see DataStorageService)
    private final Clock clock;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final PruneExpiredEntriesService pruneExpiredEntriesService = new PruneExpiredEntriesService();

    public StorageService(PersistenceService persistenceService) {
        this(persistenceService, new SystemClock());
    }

    @VisibleForTesting
    public StorageService(PersistenceService persistenceService, Clock clock) {
        this.persistenceService = persistenceService;
        this.clock = clock;

        pruneExpiredEntriesService.initialize();

        // We create all stores for those files we have already persisted.
        // Persisted data is read at the very early stages of the application start.
        Path subPath = persistenceService.getAppDataDirPath().resolve(DbSubDirectory.NETWORK_DB.getDbPath());
        try {
            String authStoreName = AUTHENTICATED_DATA_STORE.getStoreName();
            Path dirPath = subPath.resolve(authStoreName);
            if (Files.exists(dirPath)) {
                getExistingStoreKeys(dirPath, AUTHENTICATED_DATA_STORE)
                        .forEach(storeKey -> {
                            AuthenticatedDataStorageService dataStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, authStoreName, storeKey, clock);
                            AuthenticatedDataStorageService.Listener listener = createAuthenticatedDataStoreListener();
                            dataStore.addListener(listener);
                            authenticatedDataStoresListeners.put(storeKey, listener);
                            authenticatedDataStores.put(storeKey, dataStore);
                        });
            }
            String mailboxStoreName = MAILBOX_DATA_STORE.getStoreName();
            dirPath = subPath.resolve(mailboxStoreName);
            if (Files.exists(dirPath)) {
                getExistingStoreKeys(dirPath, MAILBOX_DATA_STORE)
                        .forEach(storeKey -> {
                            MailboxDataStorageService dataStore = new MailboxDataStorageService(persistenceService, pruneExpiredEntriesService, mailboxStoreName, storeKey, clock);
                            MailboxDataStorageService.Listener listener = new MailboxDataStorageService.Listener() {
                                @Override
                                public void onAdded(MailboxData mailboxData) {
                                    listeners.forEach(listener -> {
                                        try {
                                            listener.onAdded(mailboxData);
                                        } catch (Exception e) {
                                            log.error("Calling onAdded at listener {} failed", listener, e);
                                        }
                                    });
                                }

                                @Override
                                public void onRemoved(MailboxData mailboxData) {
                                    listeners.forEach(listener -> {
                                        try {
                                            listener.onRemoved(mailboxData);
                                        } catch (Exception e) {
                                            log.error("Calling onRemoved at listener {} failed", listener, e);
                                        }
                                    });
                                }
                            };
                            dataStore.addListener(listener);
                            mailboxStoresListeners.put(storeKey, listener);
                            mailboxStores.put(storeKey, dataStore);
                        });
            }

            String appendStoreName = APPEND_ONLY_DATA_STORE.getStoreName();
            dirPath = subPath.resolve(appendStoreName);
            if (Files.exists(dirPath)) {
                getExistingStoreKeys(dirPath, APPEND_ONLY_DATA_STORE)
                        .forEach(storeKey -> {
                            AppendOnlyDataStorageService dataStore = new AppendOnlyDataStorageService(persistenceService, appendStoreName, storeKey);
                            AppendOnlyDataStorageService.Listener listener = appendOnlyData -> listeners.forEach(l -> {
                                try {
                                    l.onAdded(appendOnlyData);
                                } catch (Exception e) {
                                    log.error("Calling onAdded at listener {} failed", l, e);
                                }
                            });
                            dataStore.addListener(listener);
                            appendOnlyDataStoresListeners.put(storeKey, listener);
                            appendOnlyDataStores.put(storeKey, dataStore);
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        authenticatedDataStores.forEach((key, store) -> {
            AuthenticatedDataStorageService.Listener listener = authenticatedDataStoresListeners.get(key);
            store.removeListener(listener);
        });
        authenticatedDataStoresListeners.clear();

        mailboxStores.forEach((key, store) -> {
            MailboxDataStorageService.Listener listener = mailboxStoresListeners.get(key);
            store.removeListener(listener);
        });
        mailboxStoresListeners.clear();

        appendOnlyDataStores.forEach((key, store) -> {
            AppendOnlyDataStorageService.Listener listener = appendOnlyDataStoresListeners.get(key);
            store.removeListener(listener);
        });
        appendOnlyDataStoresListeners.clear();

        authenticatedDataStores.values().forEach(DataStorageService::shutdown);
        authenticatedDataStores.clear();

        mailboxStores.values().forEach(DataStorageService::shutdown);
        mailboxStores.clear();

        appendOnlyDataStores.values().forEach(DataStorageService::shutdown);
        appendOnlyDataStores.clear();
        listeners.clear();
    }


    /* --------------------------------------------------------------------- */
    // Get AuthenticatedData
    /* --------------------------------------------------------------------- */

    public Stream<AuthenticatedData> getAuthenticatedData() {
        return authenticatedDataStores.values().stream().flatMap(this::getAuthenticatedData);
    }

    public Stream<AuthenticatedData> getAuthenticatedData(String storeKey) {
        return getAuthenticatedData(getStoreByFileName(storeKey));
    }

    private Stream<AuthenticatedData> getAuthenticatedData(Stream<DataStorageService<? extends DataRequest>> stores) {
        return stores.flatMap(this::getAuthenticatedData);
    }

    private Stream<AuthenticatedData> getAuthenticatedData(DataStorageService<? extends DataRequest> store) {
        // todo use getClone once tested enough on main
        return store.getPersistableStore().getMutableClone().getMap().values().stream()
                .filter(e -> e instanceof AddAuthenticatedDataRequest)
                .map(e -> ((AddAuthenticatedDataRequest) e).getAuthenticatedSequentialData())
                .filter(sequentialData -> isPublishedByOwner(sequentialData.getAuthenticatedData(),
                        sequentialData.getPubKeyHash()))
                .map(AuthenticatedSequentialData::getAuthenticatedData);
    }

    // Applications get only the data of entries which the owner of the data published (see
    // docs/specifications/network/storage-ownership.md). During the transition of the publisher of authorized data, a
    // store also holds entries of other publishers. Applications identify data without its publisher. So the removal
    // or expiry of such an entry would remove the data of the owner from them. The stores still hold and relay these
    // entries.
    private static boolean isPublishedByOwner(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
        return authenticatedData.isOwner(publisherPubKeyHash);
    }


    /* --------------------------------------------------------------------- */
    // Add data
    /* --------------------------------------------------------------------- */

    public CompletableFuture<DataStorageResult> onAddDataRequest(AddDataRequest addDataRequest) {
        if (addDataRequest instanceof AddMailboxRequest) {
            return onAddMailboxRequest((AddMailboxRequest) addDataRequest);
        } else if (addDataRequest instanceof AddAuthenticatedDataRequest) {
            return onAddAuthenticatedDataRequest((AddAuthenticatedDataRequest) addDataRequest);
        } else if (addDataRequest instanceof AddAppendOnlyDataRequest) {
            return onAddAppendOnlyDataRequest((AddAppendOnlyDataRequest) addDataRequest);
        } else {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("AddRequest called with invalid addDataRequest: " +
                            addDataRequest.getClass().getSimpleName()));
        }
    }

    private CompletableFuture<DataStorageResult> onAddMailboxRequest(AddMailboxRequest request) {
        MailboxData mailboxData = request.getMailboxSequentialData().getMailboxData();
        return getOrCreateMailboxDataStore(mailboxData.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSevereFailure()) {
                        log.info("AddMailboxRequest was not added to store. Result={}", dataStorageResult);
                    }
                    return dataStorageResult;
                });
    }

    private CompletableFuture<DataStorageResult> onAddAuthenticatedDataRequest(AddAuthenticatedDataRequest request) {
        AuthenticatedData authenticatedData = request.getAuthenticatedSequentialData().getAuthenticatedData();
        return getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSevereFailure()) {
                        log.info("AddAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                    }
                    return dataStorageResult;
                });
    }

    private CompletableFuture<DataStorageResult> onAddAppendOnlyDataRequest(AddAppendOnlyDataRequest request) {
        AppendOnlyData appendOnlyData = request.getAppendOnlyData();
        return getOrCreateAppendOnlyDataStore(appendOnlyData.getMetaData().getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSevereFailure()) {
                        log.info("AddAppendOnlyDataRequest was not added to store. Result={}", dataStorageResult);
                    }
                    return dataStorageResult;
                });
    }


    /* --------------------------------------------------------------------- */
    // Remove data
    /* --------------------------------------------------------------------- */

    // A removal can be stored without removing data: when it arrives before the add request, and when it replaces a
    // stored removal. The store listeners report only removed data to the applications.
    public CompletableFuture<DataStorageResult> onRemoveDataRequest(RemoveDataRequest removeDataRequest) {
        if (removeDataRequest instanceof RemoveMailboxRequest) {
            return onRemoveMailboxRequest((RemoveMailboxRequest) removeDataRequest);
        } else if (removeDataRequest instanceof RemoveAuthenticatedDataRequest) {
            return onRemoveAuthenticatedDataRequest((RemoveAuthenticatedDataRequest) removeDataRequest);
        } else {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("AddRequest called with invalid addDataRequest: " +
                            removeDataRequest.getClass().getSimpleName()));
        }
    }

    private CompletableFuture<DataStorageResult> onRemoveMailboxRequest(RemoveMailboxRequest request) {
        return getOrCreateMailboxDataStore(request.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.remove(request);
                    if (dataStorageResult.isSevereFailure()) {
                        log.info("RemoveMailboxRequest was not added to store. Result={}", dataStorageResult);
                    }
                    return dataStorageResult;
                });
    }

    private CompletableFuture<DataStorageResult> onRemoveAuthenticatedDataRequest(RemoveAuthenticatedDataRequest request) {
        return getOrCreateAuthenticatedDataStore(request.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.remove(request);
                    if (dataStorageResult.isSevereFailure()) {
                        log.info("RemoveAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                    }
                    return dataStorageResult;
                });
    }


    /* --------------------------------------------------------------------- */
    // Refresh data
    /* --------------------------------------------------------------------- */

    // A refresh request applies only to an add request which a store holds. So it never creates a store: a store
    // which does not exist holds no data, and the class name comes from the peer.
    // Returns true if the store applied the request.
    public boolean onRefreshDataRequest(RefreshAuthenticatedDataRequest request) {
        AuthenticatedDataStorageService store = authenticatedDataStores.get(request.getClassName());
        if (store == null) {
            return false;
        }
        DataStorageResult dataStorageResult = store.refresh(request);
        if (dataStorageResult.isSevereFailure()) {
            log.info("RefreshAuthenticatedDataRequest was not applied. Result={}", dataStorageResult);
        }
        return dataStorageResult.isSuccess();
    }

    public Stream<Map<ByteArray, AuthenticatedDataRequest>> getAuthenticatedDataStoreMaps() {
        // todo use getClone once tested enough on main
        return authenticatedDataStores.values().stream().map(store -> store.getPersistableStore().getMutableClone().getMap());
    }

    public Stream<Map<ByteArray, MailboxRequest>> getMailboxStoreMaps() {
        // todo use getClone once tested enough on main
        return mailboxStores.values().stream().map(store -> store.getPersistableStore().getMutableClone().getMap());
    }

    public Stream<Map<ByteArray, AddAppendOnlyDataRequest>> getAddAppendOnlyDataStoreMaps() {
        // todo use getClone once tested enough on main
        return appendOnlyDataStores.values().stream().map(store -> store.getPersistableStore().getMutableClone().getMap());
    }

    public Stream<Map.Entry<ByteArray, ? extends DataRequest>> getAllDataRequestMapEntries() {
        return getStoresByStoreType(ALL).flatMap(store -> new HashMap<>(store.getPersistableStore().getMap()).entrySet().stream());
    }

    public long getNetworkDatabaseSize() {
        return getStoresByStoreType(ALL)
                .mapToLong(store -> store.getPersistableStore().getSerializedSize())
                .sum();
    }

    public Stream<MailboxData> getMailboxData() {
        return mailboxStores.values().stream().flatMap(this::getMailboxData);
    }

    private Stream<MailboxData> getMailboxData(DataStorageService<? extends DataRequest> store) {
        // todo use getClone once tested enough on main
        return store.getPersistableStore().getMutableClone().getMap().values().stream()
                .filter(e -> e instanceof AddMailboxRequest)
                .map(e -> (AddMailboxRequest) e)
                .map(e -> e.getMailboxSequentialData().getMailboxData());
    }


    /* --------------------------------------------------------------------- */
    // Get or create stores
    /* --------------------------------------------------------------------- */

    public synchronized CompletableFuture<AuthenticatedDataStorageService> getOrCreateAuthenticatedDataStore(String storeKey) {
        if (isStoreKeyNotAllowed(storeKey, AUTHENTICATED_DATA_STORE)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unsupported storeKey"));
        }
        if (!authenticatedDataStores.containsKey(storeKey)) {
            AuthenticatedDataStorageService storageService = new AuthenticatedDataStorageService(persistenceService,
                    pruneExpiredEntriesService,
                    AUTHENTICATED_DATA_STORE.getStoreName(),
                    storeKey,
                    clock);

            AuthenticatedDataStorageService.Listener listener = createAuthenticatedDataStoreListener();
            storageService.addListener(listener);
            authenticatedDataStoresListeners.put(storeKey, listener);
            storageService.readPersisted();
            authenticatedDataStores.put(storeKey, storageService);
            return CompletableFuture.completedFuture(storageService);
        } else {
            return CompletableFuture.completedFuture(authenticatedDataStores.get(storeKey));
        }
    }

    public synchronized CompletableFuture<MailboxDataStorageService> getOrCreateMailboxDataStore(String storeKey) {
        if (isStoreKeyNotAllowed(storeKey, MAILBOX_DATA_STORE)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unsupported storeKey"));
        }
        if (!mailboxStores.containsKey(storeKey)) {
            MailboxDataStorageService storageService = new MailboxDataStorageService(persistenceService,
                    pruneExpiredEntriesService,
                    MAILBOX_DATA_STORE.getStoreName(),
                    storeKey,
                    clock);

            MailboxDataStorageService.Listener listener = new MailboxDataStorageService.Listener() {
                @Override
                public void onAdded(MailboxData mailboxData) {
                    listeners.forEach(listener -> {
                        try {
                            listener.onAdded(mailboxData);
                        } catch (Exception e) {
                            log.error("Calling onAdded at listener {} failed", listener, e);
                        }
                    });
                }

                @Override
                public void onRemoved(MailboxData mailboxData) {
                    listeners.forEach(listener -> {
                        try {
                            listener.onRemoved(mailboxData);
                        } catch (Exception e) {
                            log.error("Calling onRemoved at listener {} failed", listener, e);
                        }
                    });
                }
            };

            storageService.addListener(listener);
            mailboxStoresListeners.put(storeKey, listener);
            storageService.readPersisted();
            mailboxStores.put(storeKey, storageService);
            return CompletableFuture.completedFuture(storageService);


        } else {
            return CompletableFuture.completedFuture(mailboxStores.get(storeKey));
        }
    }

    public synchronized CompletableFuture<AppendOnlyDataStorageService> getOrCreateAppendOnlyDataStore(String storeKey) {
        if (isStoreKeyNotAllowed(storeKey, APPEND_ONLY_DATA_STORE)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unsupported storeKey"));
        }
        if (!appendOnlyDataStores.containsKey(storeKey)) {
            AppendOnlyDataStorageService storageService = new AppendOnlyDataStorageService(persistenceService,
                    APPEND_ONLY_DATA_STORE.getStoreName(),
                    storeKey);

            AppendOnlyDataStorageService.Listener listener = appendOnlyData -> listeners.forEach(l -> {
                try {
                    l.onAdded(appendOnlyData);
                } catch (Exception e) {
                    log.error("Calling onAdded at listener {} failed", l, e);
                }
            });

            storageService.addListener(listener);
            appendOnlyDataStoresListeners.put(storeKey, listener);
            storageService.readPersisted();
            appendOnlyDataStores.put(storeKey, storageService);
            return CompletableFuture.completedFuture(storageService);
        } else {
            return CompletableFuture.completedFuture(appendOnlyDataStores.get(storeKey));
        }
    }

    // Forwards the events of an authenticated data store to the listeners of this service, for data which its owner
    // published
    private AuthenticatedDataStorageService.Listener createAuthenticatedDataStoreListener() {
        return new AuthenticatedDataStorageService.Listener() {
            @Override
            public void onAdded(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                if (!isPublishedByOwner(authenticatedData, publisherPubKeyHash)) {
                    return;
                }
                listeners.forEach(listener -> {
                    try {
                        listener.onAdded(authenticatedData);
                    } catch (Exception e) {
                        log.error("Calling onAdded at listener {} failed", listener, e);
                    }
                });
            }

            @Override
            public void onRemoved(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                if (!isPublishedByOwner(authenticatedData, publisherPubKeyHash)) {
                    return;
                }
                listeners.forEach(listener -> {
                    try {
                        listener.onRemoved(authenticatedData);
                    } catch (Exception e) {
                        log.error("Calling onRemoved at listener {} failed", listener, e);
                    }
                });
            }

            @Override
            public void onRefreshed(AuthenticatedData authenticatedData, byte[] publisherPubKeyHash) {
                if (!isPublishedByOwner(authenticatedData, publisherPubKeyHash)) {
                    return;
                }
                listeners.forEach(listener -> {
                    try {
                        listener.onRefreshed(authenticatedData);
                    } catch (Exception e) {
                        log.error("Calling onRefresh at listener {} failed", listener, e);
                    }
                });
            }
        };
    }


    /* --------------------------------------------------------------------- */
    // Get stores
    /* --------------------------------------------------------------------- */

    private Stream<DataStorageService<? extends DataRequest>> getAllStores() {
        return Stream.concat(Stream.concat(authenticatedDataStores.values().stream(),
                        mailboxStores.values().stream()),
                appendOnlyDataStores.values().stream());
    }

    public Stream<DataStorageService<? extends DataRequest>> getStoresByStoreType(StoreType storeType) {
        List<DataStorageService<? extends DataRequest>> dataStorageServiceStream = switch (storeType) {
            case ALL -> getAllStores().collect(Collectors.toList());
            case AUTHENTICATED_DATA_STORE -> new ArrayList<>(authenticatedDataStores.values());
            case MAILBOX_DATA_STORE -> new ArrayList<>(mailboxStores.values());
            case APPEND_ONLY_DATA_STORE -> new ArrayList<>(appendOnlyDataStores.values());
        };
        return dataStorageServiceStream.stream();
    }

    private Stream<DataStorageService<? extends DataRequest>> getStoreByFileName(String storeKey) {
        return getAllStores()
                .filter(store -> storeKey.equals(store.getStoreKey()));
    }

    // A store holds the data of one type, and its key is the class name of that type. The storage layer takes the
    // MetaData of a store only from the StorableTypesRegistry, so it does not accept a type which is not registered
    // there. It also does not accept a type which is registered for another store type. Otherwise a mailbox request
    // could name a DistributedData type and get its MetaData.
    private static boolean isStoreKeyNotAllowed(String storeKey, StoreType storeType) {
        return !StorableTypesRegistry.isRegistered(storeKey, storeType);
    }

    private Set<String> getExistingStoreKeys(Path dirPath, StoreType storeType) {
        return StorableTypesRegistry.getClassNames(storeType).stream()
                .filter(className -> {
                    String storageFileName = StringUtils.camelCaseToSnakeCase(className + DataStorageService.STORE_POST_FIX) + Persistence.EXTENSION;
                    return Files.exists(dirPath.resolve(storageFileName));
                })
                .collect(Collectors.toSet());
    }


    /* --------------------------------------------------------------------- */
    // Listeners
    /* --------------------------------------------------------------------- */

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
