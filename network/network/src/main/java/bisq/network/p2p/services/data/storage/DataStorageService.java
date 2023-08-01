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
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class DataStorageService<T extends DataRequest> extends RateLimitedPersistenceClient<DataStore<T>> {
    public static final String SUB_PATH = "db" + File.separator + "network";
    public static final String STORE_POST_FIX = "Store";
    public static final int MAX_MAP_SIZE = 10000;

    @Getter
    protected final Persistence<DataStore<T>> persistence;
    @Getter
    public final DataStore<T> persistableStore = new DataStore<>();
    @Getter
    private final String storeKey;
    @Getter
    protected final String subDirectory;

    public DataStorageService(PersistenceService persistenceService, String storeName, String storeKey) {
        super();
        this.storeKey = storeKey;
        String storageFileName = StringUtils.camelCaseToSnakeCase(storeKey + STORE_POST_FIX);
        subDirectory = SUB_PATH + File.separator + storeName;
        persistence = persistenceService.getOrCreatePersistence(this, subDirectory, storageFileName, persistableStore);
    }

    public void shutdown() {
    }

    @Override
    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    @Override
    public DataStore<T> prunePersisted(DataStore<T> persisted) {
        Map<ByteArray, T> map = persisted.getMap();
        Map<ByteArray, T> pruned = map.entrySet().stream()
                .filter(entry -> !entry.getValue().isExpired())
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.clear();
        map.putAll(pruned);
        return persisted;
    }
}
