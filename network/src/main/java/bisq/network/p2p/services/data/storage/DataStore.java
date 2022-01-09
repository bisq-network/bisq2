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
import bisq.network.p2p.services.data.DataRequest;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DataStore<T extends DataRequest> extends RateLimitedPersistenceClient<HashMap<ByteArray, T>> {
    public static final String SUB_PATH = "db" + File.separator + "network";
    @Getter
    protected final Persistence<HashMap<ByteArray, T>> persistence;
    protected final ConcurrentHashMap<ByteArray, T> map = new ConcurrentHashMap<>();
    @Getter
    private final String fileName;
    @Getter
    private final String subDirectory;

    public DataStore(PersistenceService persistenceService, String storeName, String fileName) {
        super();
        this.fileName = fileName;
        subDirectory = SUB_PATH + File.separator + storeName;
        persistence = persistenceService.getOrCreatePersistence(this, subDirectory, fileName);
    }

    @Override
    public void applyPersisted(HashMap<ByteArray, T> persisted) {
        synchronized (map) {
            map.putAll(persisted);
        }
    }

    @Override
    public HashMap<ByteArray, T> getClone() {
        synchronized (map) {
            return new HashMap<>(map);
        }
    }

  /*  public Inventory getInventory(DataFilter dataFilter) {
        Map<ByteArray, T> mapClone = getClone();
        List<T> result = mapClone.entrySet().stream()
                .filter(e -> dataFilter.doInclude(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        return new Inventory(result, mapClone.size() - result.size());
    }*/

    abstract public void shutdown();
}
