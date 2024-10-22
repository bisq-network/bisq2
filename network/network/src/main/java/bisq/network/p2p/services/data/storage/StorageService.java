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
import bisq.common.proto.NetworkStorageWhiteList;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.append.AppendOnlyDataStorageService;
import bisq.network.p2p.services.data.storage.auth.*;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.mailbox.*;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.StoreType.*;

@Slf4j
public class StorageService {
    public interface Listener {
        void onAdded(StorageData storageData);

        void onRemoved(StorageData storageData);

        void onRefreshed(StorageData storageData);
    }

    final Map<String, AuthenticatedDataStorageService> authenticatedDataStores = new ConcurrentHashMap<>();
    final Map<String, MailboxDataStorageService> mailboxStores = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyDataStorageService> appendOnlyDataStores = new ConcurrentHashMap<>();
    private final PersistenceService persistenceService;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final PruneExpiredEntriesService pruneExpiredEntriesService = new PruneExpiredEntriesService();

    public StorageService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;

        pruneExpiredEntriesService.initialize();

        // We create all stores for those files we have already persisted.
        // Persisted data is read at the very early stages of the application start.
        String subPath = persistenceService.getBaseDir() + File.separator + DbSubDirectory.NETWORK_DB.getDbPath();
        try {
            String authStoreName = AUTHENTICATED_DATA_STORE.getStoreName();
            String directory = subPath + File.separator + authStoreName;
            if (new File(directory).exists()) {
                getExistingStoreKeys(directory)
                        .forEach(storeKey -> {
                            AuthenticatedDataStorageService dataStore = new AuthenticatedDataStorageService(persistenceService, pruneExpiredEntriesService, authStoreName, storeKey);
                            dataStore.addListener(new AuthenticatedDataStorageService.Listener() {
                                @Override
                                public void onAdded(AuthenticatedData authenticatedData) {
                                    listeners.forEach(listener -> {
                                        try {
                                            listener.onAdded(authenticatedData);
                                        } catch (Exception e) {
                                            log.error("Calling onAdded at listener {} failed", listener, e);
                                        }
                                    });
                                }

                                @Override
                                public void onRemoved(AuthenticatedData authenticatedData) {
                                    listeners.forEach(listener -> {
                                        try {
                                            listener.onRemoved(authenticatedData);
                                        } catch (Exception e) {
                                            log.error("Calling onRemoved at listener {} failed", listener, e);
                                        }
                                    });
                                }

                                @Override
                                public void onRefreshed(AuthenticatedData authenticatedData) {
                                    listeners.forEach(listener -> {
                                        try {
                                            listener.onRefreshed(authenticatedData);
                                        } catch (Exception e) {
                                            log.error("Calling onRefresh at listener {} failed", listener, e);
                                        }
                                    });
                                }
                            });
                            authenticatedDataStores.put(storeKey, dataStore);
                        });
            }
            String mailboxStoreName = MAILBOX_DATA_STORE.getStoreName();
            directory = subPath + File.separator + mailboxStoreName;
            if (new File(directory).exists()) {
                getExistingStoreKeys(directory)
                        .forEach(storeKey -> {
                            MailboxDataStorageService dataStore = new MailboxDataStorageService(persistenceService, pruneExpiredEntriesService, mailboxStoreName, storeKey);
                            dataStore.addListener(new MailboxDataStorageService.Listener() {
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
                            });
                            mailboxStores.put(storeKey, dataStore);
                        });
            }

            String appendStoreName = APPEND_ONLY_DATA_STORE.getStoreName();
            directory = subPath + File.separator + appendStoreName;
            if (new File(directory).exists()) {
                getExistingStoreKeys(directory)
                        .forEach(storeKey -> {
                            AppendOnlyDataStorageService dataStore = new AppendOnlyDataStorageService(persistenceService, appendStoreName, storeKey);
                            dataStore.addListener(appendOnlyData -> listeners.forEach(listener -> {
                                try {
                                    listener.onAdded(appendOnlyData);
                                } catch (Exception e) {
                                    log.error("Calling onAdded at listener {} failed", listener, e);
                                }
                            }));
                            appendOnlyDataStores.put(storeKey, dataStore);
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        authenticatedDataStores.values().forEach(DataStorageService::shutdown);
        mailboxStores.values().forEach(DataStorageService::shutdown);
        appendOnlyDataStores.values().forEach(DataStorageService::shutdown);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get AuthenticatedData
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthenticatedData> getAuthenticatedData() {
        return authenticatedDataStores.values().stream().flatMap(this::getAuthenticatedData);
    }

    public Stream<AuthenticatedData> getAuthenticatedData(String storeKey) {
        return getAuthenticatedData(getStoreByFileName(storeKey));
    }

    public Stream<AuthenticatedData> getAuthenticatedData(Stream<DataStorageService<? extends DataRequest>> stores) {
        return stores.flatMap(this::getAuthenticatedData);
    }

    private Stream<AuthenticatedData> getAuthenticatedData(DataStorageService<? extends DataRequest> store) {
        return store.getPersistableStore().getClone().getMap().values().stream()
                .filter(e -> e instanceof AddAuthenticatedDataRequest)
                .map(e -> (AddAuthenticatedDataRequest) e)
                .map(e -> e.getAuthenticatedSequentialData().getAuthenticatedData());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<StorageData>> onAddDataRequest(AddDataRequest addDataRequest) {
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

    private CompletableFuture<Optional<StorageData>> onAddMailboxRequest(AddMailboxRequest request) {
        MailboxData mailboxData = request.getMailboxSequentialData().getMailboxData();
        return getOrCreateMailboxDataStore(mailboxData.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSuccess()) {
                        return Optional.of(mailboxData);
                    } else {
                        if (dataStorageResult.isSevereFailure()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<StorageData>> onAddAuthenticatedDataRequest(AddAuthenticatedDataRequest request) {
        AuthenticatedData authenticatedData = request.getAuthenticatedSequentialData().getAuthenticatedData();
        return getOrCreateAuthenticatedDataStore(authenticatedData.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSuccess()) {
                        return Optional.of(authenticatedData);
                    } else {
                        if (dataStorageResult.isSevereFailure()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<StorageData>> onAddAppendOnlyDataRequest(AddAppendOnlyDataRequest request) {
        AppendOnlyData appendOnlyData = request.getAppendOnlyData();
        return getOrCreateAppendOnlyDataStore(appendOnlyData.getMetaData().getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.add(request);
                    if (dataStorageResult.isSuccess()) {
                        return Optional.of(appendOnlyData);
                    } else {
                        if (dataStorageResult.isSevereFailure()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                        }
                        return Optional.empty();
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Remove data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<StorageData>> onRemoveDataRequest(RemoveDataRequest removeDataRequest) {
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

    private CompletableFuture<Optional<StorageData>> onRemoveMailboxRequest(RemoveMailboxRequest request) {
        return getOrCreateMailboxDataStore(request.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.remove(request);
                    if (dataStorageResult.isSuccess()) {
                        return Optional.of(dataStorageResult.getRemovedData());
                    } else {
                        if (dataStorageResult.isSevereFailure()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<StorageData>> onRemoveAuthenticatedDataRequest(RemoveAuthenticatedDataRequest request) {
        return getOrCreateAuthenticatedDataStore(request.getClassName())
                .thenApply(store -> {
                    DataStorageResult dataStorageResult = store.remove(request);
                    if (dataStorageResult.isSuccess()) {
                        return Optional.of(dataStorageResult.getRemovedData());
                    } else {
                        if (dataStorageResult.isSevereFailure()) {
                            log.warn("RemoveAuthenticatedDataRequest was not added to store. Result={}", dataStorageResult);
                        }
                        return Optional.empty();
                    }
                });
    }

    public Stream<Map<ByteArray, AuthenticatedDataRequest>> getAuthenticatedDataStoreMaps() {
        return authenticatedDataStores.values().stream().map(store -> store.getPersistableStore().getClone().getMap());
    }

    public Stream<Map<ByteArray, MailboxRequest>> getMailboxStoreMaps() {
        return mailboxStores.values().stream().map(store -> store.getPersistableStore().getClone().getMap());
    }

    public Stream<Map<ByteArray, AddAppendOnlyDataRequest>> getAddAppendOnlyDataStoreMaps() {
        return appendOnlyDataStores.values().stream().map(store -> store.getPersistableStore().getClone().getMap());
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
        return store.getPersistableStore().getClone().getMap().values().stream()
                .filter(e -> e instanceof AddMailboxRequest)
                .map(e -> (AddMailboxRequest) e)
                .map(e -> e.getMailboxSequentialData().getMailboxData());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get or create stores
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<AuthenticatedDataStorageService> getOrCreateAuthenticatedDataStore(String storeKey) {
        if (!authenticatedDataStores.containsKey(storeKey)) {
            AuthenticatedDataStorageService dataStore = new AuthenticatedDataStorageService(persistenceService,
                    pruneExpiredEntriesService,
                    AUTHENTICATED_DATA_STORE.getStoreName(),
                    storeKey);
            dataStore.addListener(new AuthenticatedDataStorageService.Listener() {
                @Override
                public void onAdded(AuthenticatedData authenticatedData) {
                    listeners.forEach(listener -> {
                        try {
                            listener.onAdded(authenticatedData);
                        } catch (Exception e) {
                            log.error("Calling onAdded at listener {} failed", listener, e);
                        }
                    });
                }

                @Override
                public void onRemoved(AuthenticatedData authenticatedData) {
                    listeners.forEach(listener -> {
                        try {
                            listener.onRemoved(authenticatedData);
                        } catch (Exception e) {
                            log.error("Calling onRemoved at listener {} failed", listener, e);
                        }
                    });
                }
            });
            authenticatedDataStores.put(storeKey, dataStore);
            return dataStore.readPersisted().thenApplyAsync(store -> dataStore, NetworkService.DISPATCHER);
        } else {
            return CompletableFuture.completedFuture(authenticatedDataStores.get(storeKey));
        }
    }

    public CompletableFuture<MailboxDataStorageService> getOrCreateMailboxDataStore(String storeKey) {
        if (!mailboxStores.containsKey(storeKey)) {
            MailboxDataStorageService dataStore = new MailboxDataStorageService(persistenceService,
                    pruneExpiredEntriesService,
                    MAILBOX_DATA_STORE.getStoreName(),
                    storeKey);
            dataStore.addListener(new MailboxDataStorageService.Listener() {
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
            });
            mailboxStores.put(storeKey, dataStore);
            return dataStore.readPersisted().thenApply(nil -> dataStore);
        } else {
            return CompletableFuture.completedFuture(mailboxStores.get(storeKey));
        }
    }

    public CompletableFuture<AppendOnlyDataStorageService> getOrCreateAppendOnlyDataStore(String storeKey) {
        if (!appendOnlyDataStores.containsKey(storeKey)) {
            AppendOnlyDataStorageService dataStore = new AppendOnlyDataStorageService(persistenceService,
                    APPEND_ONLY_DATA_STORE.getStoreName(),
                    storeKey);
            appendOnlyDataStores.put(storeKey, dataStore);
            return dataStore.readPersisted().thenApply(nil -> dataStore);
        } else {
            return CompletableFuture.completedFuture(appendOnlyDataStores.get(storeKey));
        }
    }

    public <T extends AuthorizedDistributedData> void cleanupMap(String storeKey,
                                                                 Function<AuthorizedDistributedData, Optional<T>> typeFilter) {
        try {
            AuthenticatedDataStorageService authenticatedDataStorageService = getOrCreateAuthenticatedDataStore(storeKey).join();
            Map<ByteArray, AuthenticatedDataRequest> map = authenticatedDataStorageService.getPersistableStore().getMap();
            // We create a map with the distributedData as key. The duplicated entries will get filtered by data and sequence number, so the most recent and highest seq nr is used.
            Map<T, Map.Entry<ByteArray, AuthenticatedDataRequest>> entryByDistributedData = map.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof AddAuthenticatedDataRequest) // We have only polluted AddAuthenticatedDataRequest, so we can ignore the RemoveAuthenticatedDataRequest
                    .filter(entry -> {
                        AddAuthenticatedDataRequest addAuthenticatedDataRequest = (AddAuthenticatedDataRequest) entry.getValue();
                        return addAuthenticatedDataRequest.getAuthenticatedSequentialData().getAuthenticatedData() instanceof AuthorizedData;
                    })
                    .map(entry -> {
                        AddAuthenticatedDataRequest addAuthenticatedDataRequest = (AddAuthenticatedDataRequest) entry.getValue();
                        AuthorizedData authorizedData = (AuthorizedData) addAuthenticatedDataRequest.getAuthenticatedSequentialData().getAuthenticatedData();
                        return typeFilter.apply(authorizedData.getAuthorizedDistributedData())
                                .map(distributedData -> Maps.immutableEntry(distributedData, entry))
                                .orElse(null);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> {
                                if (newValue.getValue().getSequenceNumber() > oldValue.getValue().getSequenceNumber()) {
                                    return newValue;
                                }
                                if (newValue.getValue().getSequenceNumber() == oldValue.getValue().getSequenceNumber()) {
                                    if (newValue.getValue().getCreated() > oldValue.getValue().getCreated()) {
                                        return newValue;
                                    } else {
                                        return oldValue;
                                    }
                                } else {
                                    return oldValue;
                                }
                            }));

            Map<ByteArray, AuthenticatedDataRequest> cleaned = entryByDistributedData.values().stream()
                    .map(entry -> Maps.immutableEntry(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> {
                                // We should not have merge conflicts
                                if (newValue.getSequenceNumber() > oldValue.getSequenceNumber()) {
                                    return newValue;
                                }
                                if (newValue.getSequenceNumber() == oldValue.getSequenceNumber()) {
                                    if (newValue.getCreated() > oldValue.getCreated()) {
                                        return newValue;
                                    } else {
                                        return oldValue;
                                    }
                                } else {
                                    return oldValue;
                                }
                            }));
            log.info("cleanupMap for {}: size of cleaned map {}; size of original map={}", storeKey, cleaned.size(), map.size());
            map.clear();
            map.putAll(cleaned);
            authenticatedDataStorageService.persist();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get stores
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    private Set<String> getExistingStoreKeys(String directory) {
        return NetworkStorageWhiteList.getClassNames().stream()
                .filter(className -> {
                    String storageFileName = StringUtils.camelCaseToSnakeCase(className + DataStorageService.STORE_POST_FIX) + Persistence.EXTENSION;
                    return Path.of(directory, storageFileName).toFile().exists();
                })
                .collect(Collectors.toSet());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
