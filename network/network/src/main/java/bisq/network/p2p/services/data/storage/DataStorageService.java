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
import bisq.common.timer.Clock;
import bisq.common.timer.SystemClock;
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.persistence.backup.MaxBackupSize;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
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
    // The time of the node. It decides whether the node requires signature V2 (see
    // SignedRequest.SIGNATURE_V1_CUTOFF_DATE).
    protected final Clock clock;

    public DataStorageService(PersistenceService persistenceService, String storeName, String storeKey) {
        this(persistenceService, storeName, storeKey, new SystemClock());
    }

    public DataStorageService(PersistenceService persistenceService, String storeName, String storeKey, Clock clock) {
        super();

        this.clock = clock;
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

    // Earlier versions persisted the entries under storage key V1. The storage services look entries up with the
    // storage key of the received request, so every entry must be stored under that key. We recompute it from the
    // persisted request instead of trusting the persisted key.
    @Override
    public DataStore<T> preProcessPersisted(DataStore<T> persisted) {
        Map<ByteArray, T> map = persisted.getMap();
        if (map.isEmpty()) {
            return persisted;
        }

        Map<ByteArray, T> rekeyed = new HashMap<>();
        map.values().forEach(dataRequest -> {
            try {
                rekeyed.merge(dataRequest.getStorageKey(), dataRequest, DataStorageService::selectNewer);
            } catch (Exception e) {
                log.warn("Could not compute storage key of persisted entry {}. We drop it.", dataRequest, e);
            }
        });
        map.clear();
        map.putAll(rekeyed);
        return persisted;
    }

    // Two persisted requests only get the same key if the persisted keys were not the keys of their requests. Then we
    // keep the request which the store would keep: the higher sequence number, else the later creation date.
    private static <T extends DataRequest> T selectNewer(T existing, T candidate) {
        if (existing instanceof Sequential existingSequential && candidate instanceof Sequential candidateSequential &&
                existingSequential.getSequenceNumber() != candidateSequential.getSequenceNumber()) {
            return candidateSequential.getSequenceNumber() > existingSequential.getSequenceNumber() ? candidate : existing;
        }
        return candidate.getCreated() > existing.getCreated() ? candidate : existing;
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
                    boolean isToPrune = isToPrune(dataRequest);
                    if (isToPrune) {
                        addPrunedRequest(dataRequest);
                    }
                    return !isToPrune;
                })
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.clear();
        map.putAll(pruned);
        return persisted;
    }

    protected void addPrunedRequest(T dataRequest) {
        prunedAndExpiredDataRequests.add(dataRequest);
    }

    // A store keeps an entry until it expires. From SignedRequest.SIGNATURE_V1_CUTOFF_DATE on, a store also removes the
    // entries without signature V2 which it accepted before that date. It would not accept them anymore, and other
    // nodes do not accept them from its inventory (see docs/specifications/network/storage-request-signatures.md).
    protected boolean isToPrune(T dataRequest) {
        return dataRequest.isExpired() ||
                (dataRequest instanceof SignedRequest signedRequest &&
                        signedRequest.isRequiredSignatureV2Missing(clock.now()));
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

    // Whether one more entry would exceed the limit. The stores call it before they add an entry under a new key.
    protected boolean isExceedingMapSize() {
        int size = persistableStore.getMap().size();
        boolean isExceeding = size >= MAX_MAP_SIZE;
        if (isExceeding) {
            log.warn("Max. map size reached for {}. map.size()={}, MAX_MAP_SIZE={}", storeKey, size, MAX_MAP_SIZE);
        }
        if (size > 20_000) {
            log.info("Map size for {} reached > 20 000 entries. map.size()={}", storeKey, size);
        }
        return isExceeding;
    }
}
