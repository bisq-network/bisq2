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


import network.misq.network.p2p.services.data.storage.append.AppendOnlyDataStore;
import network.misq.network.p2p.services.data.storage.auth.AuthenticatedDataStore;
import network.misq.network.p2p.services.data.storage.mailbox.DataStore;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

public class Storage {
    public static final String DIR = File.separator + "db" + File.separator + "network";

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

    public AuthenticatedDataStore getAuthenticatedDataStore(MetaData metaData) throws IOException {
        String key = metaData.getFileName();
        if (!authenticatedDataStores.containsKey(key)) {
            authenticatedDataStores.put(key, new AuthenticatedDataStore(storageDirPath, metaData));
        }
        return authenticatedDataStores.get(key);
    }

    public MailboxDataStore getMailboxStore(MetaData metaData) throws IOException {
        String key = metaData.getFileName();
        if (!mailboxStores.containsKey(key)) {
            mailboxStores.put(key, new MailboxDataStore(storageDirPath, metaData));
        }
        return mailboxStores.get(key);
    }

    public AppendOnlyDataStore getAppendOnlyDataStore(MetaData metaData) throws IOException {
        String key = metaData.getFileName();
        if (!appendOnlyDataStores.containsKey(key)) {
            appendOnlyDataStores.put(key, new AppendOnlyDataStore(storageDirPath, metaData));
        }
        return appendOnlyDataStores.get(key);
    }


    public void shutdown() {
        authenticatedDataStores.values().forEach(DataStore::shutdown);
        mailboxStores.values().forEach(DataStore::shutdown);
        appendOnlyDataStores.values().forEach(DataStore::shutdown);
    }
}
