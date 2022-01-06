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

package bisq.network.p2p.services.data.storage.mailbox;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DataStore<T extends Serializable> extends RateLimitedPersistenceClient<HashMap<ByteArray, T>> {
    @Getter
    protected final Persistence<HashMap<ByteArray, T>> persistence;
    protected final ConcurrentHashMap<ByteArray, T> map = new ConcurrentHashMap<>();

    public DataStore(PersistenceService persistenceService, MetaData metaData) {
        super();
        String subDirectory = "db" + File.separator + "network" + File.separator + getStoreDir();
        persistence = persistenceService.getOrCreatePersistence(this, subDirectory, getFileName(metaData));
    }

    @Override
    public void applyPersisted(HashMap<ByteArray, T> persisted) {
        synchronized (map) {
            map.putAll(persisted);
        }
    }

    @Override
    public HashMap<ByteArray, T> getCloneForPersistence() {
        synchronized (map) {
            return new HashMap<>(map);
        }
    }

    protected String getStoreDir() {
        return this.getClass().getSimpleName().replace("Store", "").toLowerCase();
    }

    protected String getFileName(MetaData metaData) {
        return metaData.getFileName() + "s";
    }

    abstract public void shutdown();
}
