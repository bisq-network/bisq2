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
import bisq.persistence.PersistableStore;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//todo implement proto support after persistence is done
@Slf4j
@ToString
public class DataStore<T> implements PersistableStore<DataStore<T>> {
    @Getter
    private final Map<ByteArray, T> map = new ConcurrentHashMap<>();

    public DataStore() {
    }

    public DataStore(Map<ByteArray, T> map) {
        this.map.putAll(map);
    }

    @Override
    public void applyPersisted(DataStore<T> persisted) {
        map.clear();
        map.putAll(persisted.getMap());
    }

    @Override
    public DataStore<T> getClone() {
        return new DataStore<>(map);
    }
}