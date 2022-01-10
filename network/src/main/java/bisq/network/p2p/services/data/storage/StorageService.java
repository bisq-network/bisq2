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
import bisq.network.p2p.services.data.filter.FilterEntry;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyDataStore;
import bisq.network.p2p.services.data.storage.append.AppendOnlyPayload;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStore;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStore;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
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

import static bisq.network.p2p.services.data.storage.StorageService.StoreType.*;

@Slf4j
public class StorageService {
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

    public StorageService(PersistenceService persistenceService) {
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

    public void shutdown() {
        authenticatedDataStores.values().forEach(DataStore::shutdown);
        mailboxStores.values().forEach(DataStore::shutdown);
        appendOnlyDataStores.values().forEach(DataStore::shutdown);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthenticatedPayload> getAllAuthenticatedPayload() {
        return authenticatedDataStores.values().stream().flatMap(this::getAuthenticatedPayloadStream);
    }

    public Stream<AuthenticatedPayload> getAuthenticatedPayloadStream(String storeName) {
        return getAuthenticatedPayloadStream(getStoreByStoreName(storeName));
    }

    public Stream<AuthenticatedPayload> getAuthenticatedPayloadStream(Stream<DataStore<? extends DataRequest>> stores) {
        return stores.flatMap(this::getAuthenticatedPayloadStream);
    }

    private Stream<AuthenticatedPayload> getAuthenticatedPayloadStream(DataStore<? extends DataRequest> store) {
        return store.getClone().values().stream()
                .filter(e -> e instanceof AddAuthenticatedDataRequest)
                .map(e -> (AddAuthenticatedDataRequest) e)
                .map(e -> e.getAuthenticatedData().getPayload());
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

    private CompletableFuture<Optional<NetworkPayload>> onAddMailboxRequest(AddMailboxRequest request) {
        MailboxPayload payload = request.getMailboxData().getMailboxPayload();
        return getOrCreateMailboxDataStore(payload.getMetaData())
                .thenApply(store -> {
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        return Optional.of(payload);
                    } else {
                        if (result.isSevereFailure()) {
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
                        if (result.isSevereFailure()) {
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
                        if (result.isSevereFailure()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Remove data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<NetworkPayload>> onRemoveDataRequest(RemoveDataRequest removeDataRequest) {
        if (removeDataRequest instanceof RemoveMailboxRequest removeMailboxRequest) {
            return onRemoveMailboxRequest(removeMailboxRequest);
        } else if (removeDataRequest instanceof RemoveAuthenticatedDataRequest removeAuthenticatedDataRequest) {
            return onRemoveAuthenticatedDataRequest(removeAuthenticatedDataRequest);
        } else {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("AddRequest called with invalid addDataRequest: " +
                            removeDataRequest.getClass().getSimpleName()));
        }
    }

    private CompletableFuture<Optional<NetworkPayload>> onRemoveMailboxRequest(RemoveMailboxRequest request) {
        return getOrCreateMailboxDataStore(request.getMetaData())
                .thenApply(store -> {
                    Result result = store.remove(request);
                    if (result.isSuccess()) {
                        return Optional.of(result.getRemovedPayload());
                    } else {
                        if (result.isSevereFailure()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<NetworkPayload>> onRemoveAuthenticatedDataRequest(RemoveAuthenticatedDataRequest request) {
        return getOrCreateAuthenticatedDataStore(request.getMetaData())
                .thenApply(store -> {
                    Result result = store.remove(request);
                    if (result.isSuccess()) {
                        return Optional.of(result.getRemovedPayload());
                    } else {
                        if (result.isSevereFailure()) {
                            log.warn("RemoveAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Inventory getInventoryOfAllStores(DataFilter dataFilter) {
        return getInventory(dataFilter, getAllStores()
                .flatMap(store -> store.getClone().entrySet().stream()).collect(Collectors.toSet()));
    }

    public Inventory getInventoryFromStore(DataFilter dataFilter, DataStore<? extends DataRequest> store) {
        return getInventory(dataFilter, store.getClone().entrySet());
    }

    private Inventory getInventory(DataFilter dataFilter,
                                   Set<? extends Map.Entry<ByteArray, ? extends DataRequest>> entrySet) {
        HashSet<? extends DataRequest> result = entrySet.stream()
                .filter(mapEntry -> !dataFilter.filterEntries().contains(getFilterEntry(mapEntry)))
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(HashSet::new));
        return new Inventory(result, entrySet.size() - result.size());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Hashes for Filter
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Set<FilterEntry> getFilterEntries(StoreType storeType) {
        return getFilterEntries(getStoresByStoreType(storeType));
    }

    public Set<FilterEntry> getFilterEntries(String storeName) {
        return getFilterEntries(getStoreByStoreName(storeName));
    }

    private Set<FilterEntry> getFilterEntries(Stream<DataStore<? extends DataRequest>> stores) {
        return stores.flatMap(store -> store.getClone().entrySet().stream())
                .map(this::getFilterEntry)
                .collect(Collectors.toSet());
    }

    private FilterEntry getFilterEntry(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        DataRequest dataRequest = mapEntry.getValue();
        int sequenceNumber = 0;
        byte[] hash = mapEntry.getKey().getBytes();
        if (dataRequest instanceof AddAppendOnlyDataRequest) {
            // AddAppendOnlyDataRequest does not use a seq nr.
            return new FilterEntry(hash, 0);
        } else if (dataRequest instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
            // AddMailboxRequest extends AddAuthenticatedDataRequest so its covered here as well
            sequenceNumber = addAuthenticatedDataRequest.getAuthenticatedData().getSequenceNumber();
        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest removeAuthenticatedDataRequest) {
            // RemoveMailboxRequest extends RemoveAuthenticatedDataRequest so its covered here as well
            sequenceNumber = removeAuthenticatedDataRequest.getSequenceNumber();
        }
        return new FilterEntry(hash, sequenceNumber);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get or create stores
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    private String getStoreKey(MetaData metaData) {
        return metaData.getFileName();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Get stores
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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
}
