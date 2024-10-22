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
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.persistence.backup.MaxBackupSize;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class DataStorageService<T extends DataRequest> extends RateLimitedPersistenceClient<DataStore<T>> {
    public static final String STORE_POST_FIX = "Store";

    @Getter
    protected final Persistence<DataStore<T>> persistence;
    @Getter
    public final DataStore<T> persistableStore = new DataStore<>();
    @Getter
    private final String storeKey;
    @Getter
    protected final String subDirectory;
    @Getter
    public final ObservableSet<DataRequest> prunedAndExpiredDataRequests = new ObservableSet<>();
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
                .limit(maxSize)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.clear();
        map.putAll(pruned);
        return persisted;
    }

    protected int getMaxMapSize() {
        if (maxMapSize.isEmpty()) {
            int size = persistableStore.getMap().values().stream()
                    .map(DataRequest::getMaxMapSize)
                    .findFirst()
                    .orElse(100_000);
            // Until the too low values in some MetaData are fixed we use 5000 as min size
            maxMapSize = Optional.of(Math.max(MetaData.MAX_MAP_SIZE_5000, size));
        }
        return maxMapSize.get();
    }

    protected boolean isExceedingMapSize() {
        int size = persistableStore.getMap().size();
        boolean isExceeding = size > getMaxMapSize();
        if (isExceeding) {
            String className = persistableStore.getMap().values().stream()
                    .findFirst()
                    .map(dataRequest -> {
                        if (dataRequest instanceof AddAuthenticatedDataRequest addRequest) {
                            return addRequest.getDistributedData().getClass().getSimpleName();
                        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest removeRequest) {
                            return removeRequest.getClassName();
                        } else if (dataRequest instanceof RefreshAuthenticatedDataRequest request) {
                            return request.getClassName();
                        } else if (dataRequest instanceof AddAppendOnlyDataRequest addRequest) {
                            return addRequest.getAppendOnlyData().getClass().getSimpleName();
                        } else if (dataRequest instanceof AddMailboxRequest addRequest) {
                            return addRequest.getMailboxSequentialData().getMailboxData().getClassName();
                        } else if (dataRequest instanceof RemoveMailboxRequest removeRequest) {
                            return removeRequest.getClassName();
                        }
                        return "N/A";
                    }).orElse("N/A");
            log.warn("Max. map size reached for {}. map.size()={}, getMaxMapSize={}",
                    className, size, getMaxMapSize());
        }
        if (size > 20_000) {
            String className = persistableStore.getMap().values().stream()
                    .findFirst()
                    .map(dataRequest -> {
                        if (dataRequest instanceof AddAuthenticatedDataRequest addRequest) {
                            return addRequest.getDistributedData().getClass().getSimpleName();
                        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest removeRequest) {
                            return removeRequest.getClassName();
                        } else if (dataRequest instanceof RefreshAuthenticatedDataRequest request) {
                            return request.getClassName();
                        } else if (dataRequest instanceof AddAppendOnlyDataRequest addRequest) {
                            return addRequest.getAppendOnlyData().getClass().getSimpleName();
                        } else if (dataRequest instanceof AddMailboxRequest addRequest) {
                            return addRequest.getMailboxSequentialData().getMailboxData().getClassName();
                        } else if (dataRequest instanceof RemoveMailboxRequest removeRequest) {
                            return removeRequest.getClassName();
                        }
                        return "N/A";
                    }).orElse("N/A");
            log.info("Map size for {} reached > 20 000 entries. map.size()={}", className, size);
        }
        return isExceeding;
    }
}
