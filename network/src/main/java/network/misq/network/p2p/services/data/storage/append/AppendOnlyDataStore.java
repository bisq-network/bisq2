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

package network.misq.network.p2p.services.data.storage.append;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.data.ByteArray;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.mailbox.DataStore;
import network.misq.persistence.PersistenceService;
import network.misq.security.DigestUtil;

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

    public boolean append(AppendOnlyPayload appendOnlyData) {
        synchronized (map) {
            if (map.size() > maxMapSize) {
                return false;
            }

            byte[] hash = DigestUtil.hash(appendOnlyData.serialize());
            ByteArray byteArray = new ByteArray(hash);
            if (map.containsKey(byteArray)) {
                return false;
            }

            map.put(byteArray, appendOnlyData);
        }
        persist();
        listeners.forEach(listener -> listener.onAppended(appendOnlyData));
        return true;
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
