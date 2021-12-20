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

package network.misq.network.p2p.services.data.storage.mailbox;

import network.misq.common.util.FileUtils;
import network.misq.network.p2p.services.data.storage.MapKey;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.Storage;
import network.misq.persistence.Persistence;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static java.io.File.separator;

public abstract class DataStore<T> {
    protected final String storageFilePath;
    protected final String storageDirectory;
    protected final String fileName;
    protected final Persistence persistence;
    protected final ConcurrentHashMap<MapKey, T> map = new ConcurrentHashMap<>();

    public DataStore(String appDirPath, MetaData metaData) throws IOException {
        storageDirectory = appDirPath + Storage.DIR + File.separator + getStoreDir();
        FileUtils.makeDirs(storageDirectory);
        fileName = metaData.getFileName();
        storageFilePath = storageDirectory + separator + fileName;
        this.persistence = new Persistence(storageDirectory, fileName, map);
    }

    protected String getStoreDir() {
        return this.getClass().getSimpleName().replace("DataStore", "").toLowerCase();
    }

    abstract public void shutdown();

    protected void persist() {
        persistence.persist();
    }
}
