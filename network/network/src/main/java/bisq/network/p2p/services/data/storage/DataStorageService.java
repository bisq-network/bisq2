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

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_10_000;

@Slf4j
public abstract class DataStorageService<T extends DataRequest> extends RateLimitedPersistenceClient<DataStore<T>> {
    public static final String STORE_POST_FIX = "Store";

    @Getter
    protected final Persistence<DataStore<T>> persistence;
    @Getter
    protected final DataStore<T> persistableStore = new DataStore<>();
    @Getter
    protected final String storeKey;
    @Getter
    protected final String subDirectory;
    @Getter
    protected final ObservableSet<DataRequest> prunedAndExpiredDataRequests = new ObservableSet<>();
    protected Optional<Integer> maxMapSize = Optional.empty();

    public DataStorageService(PersistenceService persistenceService, String storeName, String storeKey) {
        super();

        this.storeKey = storeKey;
        String storageFileName = storeKey + STORE_POST_FIX;
        DbSubDirectory dbSubDirectory = DbSubDirectory.NETWORK_DB;
        subDirectory = dbSubDirectory.getDbPath() + File.separator + storeName;
        persistence = persistenceService.getOrCreatePersistence(this,
                subDirectory,
                storageFileName,
                persistableStore,
                MaxBackupSize.from(dbSubDirectory));
    }

    public void shutdown() {
    }

    @Override
    public DataStore<T> prunePersisted(DataStore<T> persisted) {
        Map<ByteArray, T> map = persisted.getMap();
        if (map.isEmpty()) {
            return persisted;
        }

        int maxSize = getMaxMapSize();
        int originalSize = map.size();
        log.debug("Maybe pruning persisted data for {}: size={}, maxSize={}", storeKey, originalSize, maxSize);

        Map<String, AtomicInteger> numEntriesByDataRequest = new TreeMap<>();
        Map<ByteArray, T> newMap = map.entrySet().stream()
                .filter(entry -> {
                    T dataRequest = entry.getValue();
                    String dataRequestName = dataRequest.getClass().getSimpleName();
                    numEntriesByDataRequest
                            .computeIfAbsent(dataRequestName, k -> new AtomicInteger(0))
                            .incrementAndGet();
                    if (dataRequest.isExpired()) {
                        // log.debug("{} is expired (was created: {})", storeKey, new Date(dataRequest.getCreated()));
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparingLong((Map.Entry<ByteArray, T> e) -> e.getValue().getCreated()).reversed())
                .limit(getMaxMapSize())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (numEntriesByDataRequest.values().stream().mapToInt(AtomicInteger::get).sum() > 0) {
            log.info("Add/Remove distribution for {}: {}", storeKey, numEntriesByDataRequest);
        }

        // If no change, nothing to do
        if (newMap.size() == originalSize) {
            return persisted;
        }

        // Determine pruned/expired entries efficiently
        Set<ByteArray> retainedKeys = newMap.keySet();
        List<T> removedEntries = map.entrySet().stream()
                .filter(e -> !retainedKeys.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        prunedAndExpiredDataRequests.addAll(removedEntries);
        log.info("Pruned {} entries for {}", removedEntries.size(), storeKey);

        // Apply changes atomically
        map.clear();
        map.putAll(newMap);

        // We do not call persist and delegate it to the event when the map gets updated at initial data requests.

        return persisted;
    }

    protected boolean isExceedingMapSize() {
        Map<ByteArray, T> map = persistableStore.getMap();
        int size = map.size();
        boolean isExceeding = size > getMaxMapSize();
        if (isExceeding) {
            log.debug("Max. map size reached for {}. map.size()={}, getMaxMapSize={}",
                    storeKey, size, getMaxMapSize());
        }
        if (size > 20_000) {
            log.info("Map size for {} reached > 20 000 entries. map.size()={}", storeKey, size);
        }
        return isExceeding;
    }

    protected int getMaxMapSize() {
        return Optional.ofNullable(MetaData.MAX_MAP_SIZE_BY_CLASS_NAME.get(storeKey))
                .orElseGet(() -> Optional.ofNullable(MetaData.DEFAULT_MAX_MAP_SIZE_BY_CLASS_NAME.get(storeKey))
                        .orElseGet(() -> {
                            log.warn("We did not find the map size for {} and use the default value of {}", storeKey, MAX_MAP_SIZE_10_000);
                            return MAX_MAP_SIZE_10_000;
                        }));
    }

}
