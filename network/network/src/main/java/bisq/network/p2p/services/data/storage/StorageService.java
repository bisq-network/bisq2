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
import bisq.network.p2p.services.data.storage.mailbox.*;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.StorageService.StoreType.*;

@Slf4j
public class StorageService {
    public enum StoreType {
        ALL(""),
        AUTHENTICATED_DATA_STORE("authenticated"),
        MAILBOX_DATA_STORE("mailbox"),
        APPEND_ONLY_DATA_STORE("append");
        @Getter
        private final String storeName;

        StoreType(String storeName) {
            this.storeName = storeName;
        }
    }


    public interface Listener {
        void onAdded(StorageData storageData);

        void onRemoved(StorageData storageData);
    }

    final Map<String, AuthenticatedDataStorageService> authenticatedDataStores = new ConcurrentHashMap<>();
    final Map<String, MailboxDataStorageService> mailboxStores = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyDataStorageService> appendOnlyDataStores = new ConcurrentHashMap<>();
    private final PersistenceService persistenceService;
    private final Set<StorageService.Listener> listeners = new CopyOnWriteArraySet<>();

    public StorageService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;

        // We create all stores for those files we have already persisted.
        // Persisted data is read at the very early stages of the application start.
        String subPath = persistenceService.getBaseDir() + File.separator + NetworkService.NETWORK_DB_PATH;
        try {
            String authStoreName = AUTHENTICATED_DATA_STORE.getStoreName();
            String directory = subPath + File.separator + authStoreName;
            if (new File(directory).exists()) {
                getExistingStoreKeys(directory)
                        .forEach(storeKey -> {
                            AuthenticatedDataStorageService dataStore = new AuthenticatedDataStorageService(persistenceService, authStoreName, storeKey);
                            dataStore.addListener(new AuthenticatedDataStorageService.Listener() {
                                @Override
                                public void onAdded(AuthenticatedData authenticatedData) {
                                    listeners.forEach(listener -> listener.onAdded(authenticatedData));
                                }

                                @Override
                                public void onRemoved(AuthenticatedData authenticatedData) {
                                    listeners.forEach(listener -> listener.onRemoved(authenticatedData));
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
                            MailboxDataStorageService dataStore = new MailboxDataStorageService(persistenceService, mailboxStoreName, storeKey);
                            dataStore.addListener(new MailboxDataStorageService.Listener() {
                                @Override
                                public void onAdded(MailboxData mailboxData) {
                                    listeners.forEach(listener -> listener.onAdded(mailboxData));
                                }

                                @Override
                                public void onRemoved(MailboxData mailboxData) {
                                    listeners.forEach(listener -> listener.onRemoved(mailboxData));
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
                            dataStore.addListener(appendOnlyData -> listeners.forEach(listener -> listener.onAdded(appendOnlyData)));
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

    public void addListener(StorageService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(StorageService.Listener listener) {
        listeners.remove(listener);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get or create stores
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<AuthenticatedDataStorageService> getOrCreateAuthenticatedDataStore(String storeKey) {
        if (!authenticatedDataStores.containsKey(storeKey)) {
            AuthenticatedDataStorageService dataStore = new AuthenticatedDataStorageService(persistenceService,
                    AUTHENTICATED_DATA_STORE.getStoreName(),
                    storeKey);
            dataStore.addListener(new AuthenticatedDataStorageService.Listener() {
                @Override
                public void onAdded(AuthenticatedData authenticatedData) {
                    listeners.forEach(listener -> listener.onAdded(authenticatedData));
                }

                @Override
                public void onRemoved(AuthenticatedData authenticatedData) {
                    listeners.forEach(listener -> listener.onRemoved(authenticatedData));
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
                    MAILBOX_DATA_STORE.getStoreName(),
                    storeKey);
            dataStore.addListener(new MailboxDataStorageService.Listener() {
                @Override
                public void onAdded(MailboxData mailboxData) {
                    listeners.forEach(listener -> listener.onAdded(mailboxData));
                }

                @Override
                public void onRemoved(MailboxData mailboxData) {
                    listeners.forEach(listener -> listener.onRemoved(mailboxData));
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get stores
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Stream<DataStorageService<? extends DataRequest>> getAllStores() {
        return Stream.concat(Stream.concat(authenticatedDataStores.values().stream(),
                        mailboxStores.values().stream()),
                appendOnlyDataStores.values().stream());
    }

    private Stream<DataStorageService<? extends DataRequest>> getStoresByStoreType(StoreType storeType) {
        List<DataStorageService<? extends DataRequest>> dataStorageServiceStream;
        switch (storeType) {
            case ALL:
                dataStorageServiceStream = getAllStores().collect(Collectors.toList());
                break;
            case AUTHENTICATED_DATA_STORE:
                dataStorageServiceStream = new ArrayList<>(authenticatedDataStores.values());
                break;
            case MAILBOX_DATA_STORE:
                dataStorageServiceStream = new ArrayList<>(mailboxStores.values());
                break;
            case APPEND_ONLY_DATA_STORE:
                dataStorageServiceStream = new ArrayList<>(appendOnlyDataStores.values());
                break;
            default:
                throw new RuntimeException("Unhandled case. storeType= " + storeType);
        }
        return dataStorageServiceStream.stream();
    }

    private Stream<DataStorageService<? extends DataRequest>> getStoreByFileName(String storeKey) {
        return getAllStores()
                .filter(store -> storeKey.equals(store.getStoreKey()));
    }

    private Set<String> getExistingStoreKeys(String directory) {
        return NetworkStorageWhiteList.getClassNames().stream()
                .filter(storeKey -> {
                    String storageFileName = StringUtils.camelCaseToSnakeCase(storeKey + DataStorageService.STORE_POST_FIX) + Persistence.EXTENSION;
                    return Path.of(directory, storageFileName).toFile().exists();
                })
                .collect(Collectors.toSet());
    }
}
