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
import network.misq.network.p2p.services.data.storage.MapKey;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.mailbox.DataStore;
import network.misq.persistence.Persistence;
import network.misq.security.DigestUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Appends AppendOnlyData to a map using the hash of the AppendOnlyData as key.
 * If key already exists we return. If map size exceeds MAX_MAP_SIZE we ignore new data.
 */
@Slf4j
public class AppendOnlyDataStore extends DataStore<AppendOnlyData> {
    private static final int MAX_MAP_SIZE = 10_000_000; // in bytes

    private final int maxMapSize;

    public interface Listener {
        void onAppended(AppendOnlyData appendOnlyData);
    }

    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public AppendOnlyDataStore(String appDirPath, MetaData metaData) throws IOException {
        super(appDirPath, metaData);

        maxMapSize = MAX_MAP_SIZE / metaData.getMaxSizeInBytes();
        if (new File(storageFilePath).exists()) {
            Serializable serializable = Persistence.read(storageFilePath);
            if (serializable instanceof ConcurrentHashMap) {
                ConcurrentHashMap<MapKey, AppendOnlyData> persisted = (ConcurrentHashMap<MapKey, AppendOnlyData>) serializable;
                map.putAll(persisted);
            }
        }
    }

    public boolean append(AppendOnlyData appendOnlyData) {
        if (map.size() > maxMapSize) {
            return false;
        }

        byte[] hash = DigestUtil.hash(appendOnlyData.serialize());
        MapKey mapKey = new MapKey(hash);
        if (map.containsKey(mapKey)) {
            return false;
        }

        map.put(mapKey, appendOnlyData);
        listeners.forEach(listener -> listener.onAppended(appendOnlyData));
        persist();
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
    ConcurrentHashMap<MapKey, AppendOnlyData> getMap() {
        return map;
    }
}
