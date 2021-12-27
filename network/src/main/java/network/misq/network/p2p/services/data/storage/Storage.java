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

package network.misq.network.p2p.services.data.storage;


import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.services.data.AddDataRequest;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.network.p2p.services.data.storage.append.AppendOnlyDataStore;
import network.misq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedDataStore;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import network.misq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import network.misq.network.p2p.services.data.storage.mailbox.DataStore;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxDataStore;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

public class Storage {
    public static final String DIR = "db" + File.separator + "network";

    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    // Class name is key
    final Map<String, AuthenticatedDataStore> authenticatedDataStores = new ConcurrentHashMap<>();
    final Map<String, MailboxDataStore> mailboxStores = new ConcurrentHashMap<>();
    final Map<String, AppendOnlyDataStore> appendOnlyDataStores = new ConcurrentHashMap<>();
    private final String storageDirPath;

    public Storage(String appDirPath) {
        storageDirPath = appDirPath + separator + "db" + separator + "network";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Optional<NetworkPayload>> addRequest(AddDataRequest addDataRequest) {
        Message message = addDataRequest.message();
        if (message instanceof AddMailboxRequest addMailboxRequest) {
            return addRequest(addMailboxRequest);
        } else if (message instanceof AddAuthenticatedDataRequest addAuthenticatedDataRequest) {
            return addRequest(addAuthenticatedDataRequest);
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException("AddRequest called with invalid addDataRequest: " + addDataRequest.getClass().getSimpleName()));
        }
    }

    private CompletableFuture<Optional<NetworkPayload>> addRequest(AddMailboxRequest request) {
        MailboxPayload payload = request.getMailboxData().getMailboxPayload();
        return getOrCreateMailboxDataStore(payload.getMetaData())
                .thenApply(store -> {
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        return Optional.of(payload);
                    } else {
                        if (!result.isRequestAlreadyReceived()) {
                            log.warn("AddAuthenticatedDataRequest was not added to store. Result={}", result);
                        }
                        return Optional.empty();
                    }
                });
    }

    private CompletableFuture<Optional<NetworkPayload>> addRequest(AddAuthenticatedDataRequest request) {
        AuthenticatedPayload payload = request.getAuthenticatedData().getPayload();
        return getOrCreateAuthenticatedDataStore(payload.getMetaData())
                .thenApply(store -> {
                    Result result = store.add(request);
                    if (result.isSuccess()) {
                        return Optional.of(payload);
                    } else {
                        if (!result.isRequestAlreadyReceived()) {
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

    public CompletableFuture<AuthenticatedDataStore> getOrCreateAuthenticatedDataStore(MetaData metaData) {
        String key = metaData.getFileName();
        if (!authenticatedDataStores.containsKey(key)) {
            AuthenticatedDataStore dataStore = new AuthenticatedDataStore(storageDirPath, metaData);
            authenticatedDataStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(authenticatedDataStores.get(key));
        }
    }

    public CompletableFuture<MailboxDataStore> getOrCreateMailboxDataStore(MetaData metaData) {
        String key = metaData.getFileName();
        if (!mailboxStores.containsKey(key)) {
            MailboxDataStore dataStore = new MailboxDataStore(storageDirPath, metaData);
            mailboxStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(mailboxStores.get(key));
        }
    }

    public CompletableFuture<AppendOnlyDataStore> getOrCreateAppendOnlyDataStore(MetaData metaData) {
        String key = metaData.getFileName();
        if (!appendOnlyDataStores.containsKey(key)) {
            AppendOnlyDataStore dataStore = new AppendOnlyDataStore(storageDirPath, metaData);
            appendOnlyDataStores.put(key, dataStore);
            return dataStore.readPersisted().thenApply(__ -> dataStore);
        } else {
            return CompletableFuture.completedFuture(appendOnlyDataStores.get(key));
        }
    }
}
