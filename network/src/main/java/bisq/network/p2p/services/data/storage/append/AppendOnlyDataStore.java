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
import bisq.network.p2p.services.data.storage.DataStore;
import bisq.network.p2p.services.data.storage.Result;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Appends AppendOnlyData to a map using the hash of the AppendOnlyData as key.
 * If key already exists we return. If map size exceeds MAX_MAP_SIZE we ignore new data.
 */
@Slf4j
public class AppendOnlyDataStore extends DataStore<AddAppendOnlyDataRequest> {
    private static final int MAX_MAP_SIZE = 10_000_000; // in bytes

    public interface Listener {
        void onAppended(AppendOnlyPayload appendOnlyData);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public AppendOnlyDataStore(PersistenceService persistenceService, String storeName, String fileName) {
        super(persistenceService, storeName, fileName);
    }

    @Override
    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    public Result add(AddAppendOnlyDataRequest addAppendOnlyDataRequest) {
        AppendOnlyPayload appendOnlyPayload = addAppendOnlyDataRequest.payload();
        synchronized (map) {
            if (map.size() > MAX_MAP_SIZE) {
                return new Result(false).maxMapSizeReached();
            }

            byte[] hash = DigestUtil.hash(appendOnlyPayload.serialize());
            ByteArray byteArray = new ByteArray(hash);
            if (map.containsKey(byteArray)) {
                return new Result(false).payloadAlreadyStored();
            }

            map.put(byteArray, addAppendOnlyDataRequest);
        }
        persist();
        listeners.forEach(listener -> listener.onAppended(appendOnlyPayload));
        return new Result(true);
    }

    @Override
    public void shutdown() {
    }

    public void addListener(AppendOnlyDataStore.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(AppendOnlyDataStore.Listener listener) {
        listeners.remove(listener);
    }
}
