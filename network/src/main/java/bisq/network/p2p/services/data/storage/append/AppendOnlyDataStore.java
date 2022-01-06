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
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.Result;
import bisq.network.p2p.services.data.storage.mailbox.DataStore;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Appends AppendOnlyData to a map using the hash of the AppendOnlyData as key.
 * If key already exists we return. If map size exceeds MAX_MAP_SIZE we ignore new data.
 */
@Slf4j
public class AppendOnlyDataStore extends DataStore<AppendOnlyPayload> {
    private static final int MAX_MAP_SIZE = 10_000_000; // in bytes

    private final int maxMapSize;

    public interface Listener {
        void onAppended(AppendOnlyPayload appendOnlyData);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public AppendOnlyDataStore(PersistenceService persistenceService, MetaData metaData) {
        super(persistenceService, metaData);

        maxMapSize = MAX_MAP_SIZE / metaData.getMaxSizeInBytes();
    }

    @Override
    protected long getMaxWriteRateInMs() {
        return 1000;
    }

    public Result add(AppendOnlyPayload appendOnlyPayload) {
        synchronized (map) {
            if (map.size() > maxMapSize) {
                return new Result(false).maxMapSizeReached();
            }

            byte[] hash = DigestUtil.hash(appendOnlyPayload.serialize());
            ByteArray byteArray = new ByteArray(hash);
            if (map.containsKey(byteArray)) {
                return new Result(false).payloadAlreadyStored();
            }

            map.put(byteArray, appendOnlyPayload);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @VisibleForTesting
    Map<ByteArray, AppendOnlyPayload> getMap() {
        return map;
    }
}
