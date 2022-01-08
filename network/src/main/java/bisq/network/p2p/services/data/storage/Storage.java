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
import bisq.common.util.FileUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyDataStore;
import bisq.network.p2p.services.data.storage.append.AppendOnlyPayload;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStore;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStore;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.Storage.StoreType.*;

@Slf4j
public class Storage {
    public enum StoreType {
        ALL(""), //todo remove
        AUTHENTICATED_DATA_STORE("AuthenticatedDataStore"),
        MAILBOX_DATA_STORE("MailboxDataStore"),
        APPEND_ONLY_DATA_STORE("AppendOnlyDataStore");
        @Getter
        private final String storeName;

        StoreType(String storeName) {
            this.storeName = storeName;
        }
    }

    final Map<String, AuthenticatedDataStore> authenticatedDataStores = new ConcurrentHashMap<>();
    final Map<String, MailboxDataStore> mailboxStores = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyDataStore> appendOnlyDataStores = new ConcurrentHashMap<>();
    private final PersistenceService persistenceService;

    public Storage(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;

        // We create all stores for those files we have already persisted.
        // Persisted data is read at the very early stages of the application start.
        String subPath = persistenceService.getBaseDir() + File.separator + DataStore.SUB_PATH;
        try {
            String authStoreName = AUTHENTICATED_DATA_STORE.getStoreName();
            String directory = subPath + File.separator + authStoreName;
            if (new File(directory).exists()) {
                FileUtils.listFilesInDirectory(directory, 1)
                        .forEach(fileName -> {
                            AuthenticatedDataStore dataStore = new AuthenticatedDataStore(persistenceService, authStoreName, fileName);
                            authenticatedDataStores.put(fileName, dataStore);
                        });
            }
            String mailboxStoreName = MAILBOX_DATA_STORE.getStoreName();
            directory = subPath + File.separator + mailboxStoreName;
            if (new File(directory).exists()) {
                FileUtils.listFilesInDirectory(directory, 1)
                        .forEach(fileName -> {
                            MailboxDataStore dataStore = new MailboxDataStore(persistenceService, mailboxStoreName, fileName);
                            mailboxStores.put(fileName, dataStore);
                        });
            }

            String appendStoreName = APPEND_ONLY_DATA_STORE.getStoreName();
            directory = subPath + File.separator + appendStoreName;
            if (new File(directory).exists()) {
                FileUtils.listFilesInDirectory(directory, 1)
                        .forEach(fileName -> {
                            AppendOnlyDataStore dataStore = new AppendOnlyDataStore(persistenceService, appendStoreName, fileName);
                            appendOnlyDataStores.put(fileName, dataStore);
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Inventory getInventoryOfAllStores(DataFilter dataFilter) {
        var entrySet = getAllStores()
                .flatMap(store -> store.getClonedMap().entrySet().stream())
                .collect(Collectors.toSet());
        return getInventory(dataFilter, entrySet);
    }

    public Inventory getInventoryFromStore(DataFilter dataFilter, DataStore<? extends DataRequest> store) {
        Set<? extends Map.Entry<ByteArray, ? extends DataRequest>> entrySet = store.getClonedMap().entrySet();
        return getInventory(dataFilter, entrySet);
    }


    public Set<byte[]> getHashes(StoreType storeType) {
        return getHashes(getStoresByStoreType(storeType));
    }

    public Set<byte[]> getHashes(String storeName) {
        return getHashes(getStoreByStoreName(storeName));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Add data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<NetworkPayload>> onAddDataRequest(AddDataRequest addDataRequest) {
        if (addDataRequest instanceof AddMailboxRequest addMailboxRequest) {
            return onAddMailboxRequest(addMailboxRequest);
        } else if (addDataRequest instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
            return onAddAuthenticatedDataRequest(addAuthenticatedDataRequest);
        } else if (addDataRequest instanceof AddAppendOnlyDataRequest addAppendOnlyDataRequest) {
            return onAddAppendOnlyDataRequest(addAppendOnlyDataRequest);
        } else {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("AddRequest called with invalid addDataRequest: " +
                            addDataRequest.getClass().getSimpleName()));
        }
    }

    public CompletableFuture<Optional<NetworkPayload>> onRemoveRequest(RemoveDataRequest removeDataRequest) {
        //todo
        return CompletableFuture.completedFuture(Optional.empty());
    }

    private CompletableFuture<Optional<NetworkPayload>> onAddMailboxRequest(AddMailboxRequest request) {
        MailboxPayload payload = request.getMailboxData().getMailboxPayload();
        return getOrCreateMailboxDataStore(payload.getMetaData())
                .thenApply(store -> {
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        return Optional.of(payload);
                    } else {
                        if (!result.isRequestAlreadyReceived() && !result.isPayloadAlreadyStored()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<NetworkPayload>> onAddAuthenticatedDataRequest(AddAuthenticatedDataRequest request) {
        AuthenticatedPayload payload = request.getAuthenticatedData().getPayload();
        return getOrCreateAuthenticatedDataStore(payload.getMetaData())
                .thenApply(store -> {
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        return Optional.of(payload);
                    } else {
                        if (!result.isRequestAlreadyReceived() && !result.isPayloadAlreadyStored()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<NetworkPayload>> onAddAppendOnlyDataRequest(AddAppendOnlyDataRequest request) {
        AppendOnlyPayload payload = request.payload();
        return getOrCreateAppendOnlyDataStore(payload.getMetaData())
                .thenApply(store -> {
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        return Optional.of(payload);
                    } else {
                        if (!result.isPayloadAlreadyStored()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }


    public Stream<AuthenticatedPayload> getAllAuthenticatedPayload() {
        return authenticatedDataStores.values().stream().flatMap(this::getAuthenticatedPayload);
        
      /*  return authenticatedDataStores.values().stream()
                .flatMap(e -> e.getClonedMap().values().stream())
                .filter(e -> e instanceof AddAuthenticatedDataRequest)
                .map(e -> (AddAuthenticatedDataRequest) e)
                .map(e -> e.getAuthenticatedData().getPayload());*/
    }

    public Stream<AuthenticatedPayload> getAuthenticatedPayload(DataStore<? extends DataRequest> store) {
        return store.getClonedMap().values().stream()
                .filter(e -> e instanceof AddAuthenticatedDataRequest)
                .map(e -> (AddAuthenticatedDataRequest) e)
                .map(e -> e.getAuthenticatedData().getPayload());
    }

    public Stream<AuthenticatedPayload> getNetworkPayloads(String storeName) {
        return getNetworkPayloads(getStoreByStoreName(storeName));
    }

    public Stream<AuthenticatedPayload> getNetworkPayloads(Stream<DataStore<? extends DataRequest>> stores) {
        return stores.flatMap(this::getAuthenticatedPayload);
    }


    public CompletableFuture<AuthenticatedDataStore> getOrCreateAuthenticatedDataStore(MetaData metaData) {
        String key = getStoreKey(metaData);
        if (!authenticatedDataStores.containsKey(key)) {
            AuthenticatedDataStore dataStore = new AuthenticatedDataStore(persistenceService,
                    AUTHENTICATED_DATA_STORE.getStoreName(),
                    metaData.getFileName());
            authenticatedDataStores.put(key, dataStore);
            return dataStore.readPersisted().thenApplyAsync(__ -> dataStore, NetworkService.DISPATCHER);
        } else {
            return CompletableFuture.completedFuture(authenticatedDataStores.get(key));
        }
    }


    public CompletableFuture<MailboxDataStore> getOrCreateMailboxDataStore(MetaData metaData) {
        String key = getStoreKey(metaData);
        if (!mailboxStores.containsKey(key)) {
            MailboxDataStore dataStore = new MailboxDataStore(persistenceService,
                    MAILBOX_DATA_STORE.getStoreName(),
                    metaData.getFileName());
            mailboxStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(mailboxStores.get(key));
        }
    }

    public CompletableFuture<AppendOnlyDataStore> getOrCreateAppendOnlyDataStore(MetaData metaData) {
        String key = getStoreKey(metaData);
        if (!appendOnlyDataStores.containsKey(key)) {
            AppendOnlyDataStore dataStore = new AppendOnlyDataStore(persistenceService,
                    APPEND_ONLY_DATA_STORE.getStoreName(),
                    metaData.getFileName());
            appendOnlyDataStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(appendOnlyDataStores.get(key));
        }
    }

    public void shutdown() {
        authenticatedDataStores.values().forEach(DataStore::shutdown);
        mailboxStores.values().forEach(DataStore::shutdown);
        appendOnlyDataStores.values().forEach(DataStore::shutdown);
    }

    private Stream<DataStore<? extends DataRequest>> getAllStores() {
        return Stream.concat(Stream.concat(authenticatedDataStores.values().stream(),
                        mailboxStores.values().stream()),
                appendOnlyDataStores.values().stream());
    }

    private Stream<DataStore<? extends DataRequest>> getStoresByStoreType(StoreType storeType) {
        List<DataStore<? extends DataRequest>> dataStoreStream =
                switch (storeType) {
                    case ALL -> getAllStores().collect(Collectors.toList());
                    case AUTHENTICATED_DATA_STORE -> new ArrayList<>(authenticatedDataStores.values());
                    case MAILBOX_DATA_STORE -> new ArrayList<>(mailboxStores.values());
                    case APPEND_ONLY_DATA_STORE -> new ArrayList<>(appendOnlyDataStores.values());
                };
        return dataStoreStream.stream();
    }

    private Stream<DataStore<? extends DataRequest>> getStoreByStoreName(String storeName) {
        return getAllStores()
                .filter(store -> storeName.equals(store.getFileName()));
    }

    private Set<byte[]> getHashes(Stream<DataStore<? extends DataRequest>> stores) {
        return stores.flatMap(store -> store.getClonedMap().keySet().stream())
                .map(ByteArray::getBytes)
                .collect(Collectors.toSet());
    }

    private Inventory getInventory(DataFilter dataFilter,
                                   Set<? extends Map.Entry<ByteArray, ? extends DataRequest>> entrySet) {
        List<? extends DataRequest> result = entrySet.stream()
                .filter(e -> dataFilter.doInclude(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return new Inventory(result, entrySet.size() - result.size());
    }

    private String getStoreKey(MetaData metaData) {
        return metaData.getFileName();
    }
}
