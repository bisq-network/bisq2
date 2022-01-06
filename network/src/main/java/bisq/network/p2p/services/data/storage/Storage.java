/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.network.p2p.services.data.storage;


import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.append.AppendOnlyDataStore;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStore;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.DataStore;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStore;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.persistence.PersistenceService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
public class Storage {

    // Class name is key
    final Map<String, AuthenticatedDataStore> authenticatedDataStores = new ConcurrentHashMap<>();
    final Map<String, MailboxDataStore> mailboxStores = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyDataStore> appendOnlyDataStores = new ConcurrentHashMap<>();
    private final PersistenceService persistenceService;

    public Storage(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<NetworkPayload>> onAddRequest(AddDataRequest addDataRequest) {
        Message message = addDataRequest.message();
        if (message instanceof AddMailboxRequest addMailboxRequest) {
            return onAddRequest(addMailboxRequest);
        } else if (message instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
            return onAddRequest(addAuthenticatedDataRequest);
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException("AddRequest called with invalid addDataRequest: " + addDataRequest.getClass().getSimpleName()));
        }
    }

    private CompletableFuture<Optional<NetworkPayload>> onAddRequest(AddMailboxRequest request) {
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

    private CompletableFuture<Optional<NetworkPayload>> onAddRequest(AddAuthenticatedDataRequest request) {
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


    public void shutdown() {
        authenticatedDataStores.values().forEach(DataStore::shutdown);
        mailboxStores.values().forEach(DataStore::shutdown);
        appendOnlyDataStores.values().forEach(DataStore::shutdown);
    }

    public Stream<AuthenticatedPayload> getAllAuthenticatedPayload() {
        return authenticatedDataStores.values().stream()
                .flatMap(e -> e.getMap().values().stream())
                .filter(e -> e instanceof AddAuthenticatedDataRequest)
                .map(e -> (AddAuthenticatedDataRequest) e)
                .map(e -> e.getAuthenticatedData().getPayload());
    }

    public CompletableFuture<AuthenticatedDataStore> getOrCreateAuthenticatedDataStore(MetaData metaData) {
        String key = metaData.getFileName();
        if (!authenticatedDataStores.containsKey(key)) {
            AuthenticatedDataStore dataStore = new AuthenticatedDataStore(persistenceService, metaData);
            authenticatedDataStores.put(key, dataStore);
            return dataStore.readPersisted().thenApplyAsync(__ -> dataStore, NetworkService.DISPATCHER);
        } else {
            return CompletableFuture.completedFuture(authenticatedDataStores.get(key));
        }
    }

    public CompletableFuture<MailboxDataStore> getOrCreateMailboxDataStore(MetaData metaData) {
        String key = metaData.getFileName();
        if (!mailboxStores.containsKey(key)) {
            MailboxDataStore dataStore = new MailboxDataStore(persistenceService, metaData);
            mailboxStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(mailboxStores.get(key));
        }
    }

    public CompletableFuture<AppendOnlyDataStore> getOrCreateAppendOnlyDataStore(MetaData metaData) {
        String key = metaData.getFileName();
        if (!appendOnlyDataStores.containsKey(key)) {
            AppendOnlyDataStore dataStore = new AppendOnlyDataStore(persistenceService, metaData);
            appendOnlyDataStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(appendOnlyDataStores.get(key));
        }
    }
}
