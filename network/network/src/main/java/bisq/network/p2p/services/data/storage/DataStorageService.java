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
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.persistence.backup.MaxBackupSize;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class DataStorageService<T extends DataRequest> extends RateLimitedPersistenceClient<DataStore<T>> {
    public static final String STORE_POST_FIX = "Store";
    // Maximum number of entries of a store. This was the effective limit of all stores so far. MetaData.maxMapSize
    // is not used as limit, because its values are at most 10 000 and enforcing them could remove entries from
    // existing stores. Nodes of earlier versions use MetaData.maxMapSize as an input of the cost factor of messages.
    public static final int MAX_MAP_SIZE = 100_000;

    @Getter
    protected final Persistence<DataStore<T>> persistence;
    @Getter
    protected final DataStore<T> persistableStore = new DataStore<>();
    @Getter
    protected final String storeKey;
    @Getter
    protected final Path subDirPath;
    @Getter
    protected final ObservableSet<DataRequest> prunedAndExpiredDataRequests = new ObservableSet<>();

    public DataStorageService(PersistenceService persistenceService, String storeName, String storeKey) {
        super();

        this.storeKey = storeKey;
        String storageFileName = storeKey + STORE_POST_FIX;
        DbSubDirectory dbSubDirectory = DbSubDirectory.NETWORK_DB;
        subDirPath = dbSubDirectory.getDbPath().resolve(storeName);
        persistence = persistenceService.getOrCreatePersistence(this,
                subDirPath,
                storageFileName,
                persistableStore,
                MaxBackupSize.from(dbSubDirectory));
    }

    public void shutdown() {
        prunedAndExpiredDataRequests.clear();
    }

    @Override
    public DataStore<T> prunePersisted(DataStore<T> persisted) {
        Map<ByteArray, T> map = persisted.getMap();
        if (map.isEmpty()) {
            return persisted;
        }

        Map<ByteArray, T> pruned = map.entrySet().stream()
                .filter(entry -> {
                    T dataRequest = entry.getValue();
                    boolean isExpired = dataRequest.isExpired();
                    if (isExpired) {
                        prunedAndExpiredDataRequests.add(dataRequest);
                    }
                    return !isExpired;
                })
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.clear();
        map.putAll(pruned);
        return persisted;
    }

    // A store holds the data of one type, and its key is the simple class name of that type. The store takes the
    // MetaData of its entries from the StorableTypesRegistry by that class name.
    protected boolean isClassNameInvalid(String className) {
        if (storeKey.equals(className)) {
            return false;
        }
        log.warn("Class name {} does not match the store {}", className, storeKey);
        return true;
    }

    protected boolean isExceedingMapSize() {
        int size = persistableStore.getMap().size();
        boolean isExceeding = size > MAX_MAP_SIZE;
        if (isExceeding) {
            log.warn("Max. map size reached for {}. map.size()={}, MAX_MAP_SIZE={}", storeKey, size, MAX_MAP_SIZE);
        }
        if (size > 20_000) {
            log.info("Map size for {} reached > 20 000 entries. map.size()={}", storeKey, size);
        }
        return isExceeding;
    }
}
