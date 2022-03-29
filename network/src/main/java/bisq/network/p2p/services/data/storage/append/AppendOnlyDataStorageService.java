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

package bisq.network.p2p.services.data.storage.append;

import bisq.common.data.ByteArray;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.Result;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Appends AppendOnlyData to a map using the hash of the AppendOnlyData as key.
 * If key already exists we return. If map size exceeds MAX_MAP_SIZE we ignore new data.
 */
@Slf4j
public class AppendOnlyDataStorageService extends DataStorageService<AddAppendOnlyDataRequest> {
    private static final int MAX_MAP_SIZE = 10_000_000; // in bytes

    public interface Listener {
        void onAppended(AppendOnlyData appendOnlyData);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Object mapAccessLock = new Object();

    public AppendOnlyDataStorageService(PersistenceService persistenceService, String storeName, String fileName) {
        super(persistenceService, storeName, fileName);
    }

    @Override
    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    public Result add(AddAppendOnlyDataRequest addAppendOnlyDataRequest) {
        AppendOnlyData appendOnlyData = addAppendOnlyDataRequest.getAppendOnlyData();
        Map<ByteArray, AddAppendOnlyDataRequest> map = persistableStore.getMap();
        synchronized (mapAccessLock) {
            if (map.size() > MAX_MAP_SIZE) {
                return new Result(false).maxMapSizeReached();
            }

            byte[] hash = DigestUtil.hash(appendOnlyData.serialize());
            ByteArray byteArray = new ByteArray(hash);
            if (map.containsKey(byteArray)) {
                return new Result(false).payloadAlreadyStored();
            }

            map.put(byteArray, addAppendOnlyDataRequest);
        }
        persist();
        listeners.forEach(listener -> listener.onAppended(appendOnlyData));
        return new Result(true);
    }

    @Override
    public void shutdown() {
    }

    public void addListener(AppendOnlyDataStorageService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(AppendOnlyDataStorageService.Listener listener) {
        listeners.remove(listener);
    }
}
