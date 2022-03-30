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
import bisq.common.timer.Scheduler;
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class DataStorageService<T extends DataRequest> extends RateLimitedPersistenceClient<DataStore<T>> {
    public static final String SUB_PATH = "db" + File.separator + "network";
    @Getter
    protected final Persistence<DataStore<T>> persistence;
    @Getter
    public final DataStore<T> persistableStore = new DataStore<>();
    @Getter
    private final String fileName;
    @Getter
    protected final String subDirectory;
    private final Scheduler scheduler;

    public DataStorageService(PersistenceService persistenceService, String storeName, String fileName) {
        super();
        this.fileName = fileName;
        subDirectory = SUB_PATH + File.separator + storeName;
        persistence = persistenceService.getOrCreatePersistence(this, subDirectory, fileName, persistableStore);
        scheduler = Scheduler.run(this::pruneExpired).periodically(1, TimeUnit.HOURS);
    }

    private void pruneExpired() {
        Set<Map.Entry<ByteArray, T>> expiredEntries = persistableStore.getMap().entrySet().stream()
                .filter(entry -> entry.getValue().isExpired())
                .collect(Collectors.toSet());
        if (!expiredEntries.isEmpty()) {
            log.info("We remove {} expired entries from our map", expiredEntries.size());
        }
        expiredEntries.forEach(e -> persistableStore.getMap().remove(e.getKey()));
    }

  /*  public Inventory getInventory(DataFilter dataFilter) {
        Map<ByteArray, T> mapClone = getClone();
        List<T> result = mapClone.entrySet().stream()
                .filter(e -> dataFilter.doInclude(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return new Inventory(result, mapClone.size() - result.size());
    }*/

    public void shutdown() {
        scheduler.stop();
    }
}
