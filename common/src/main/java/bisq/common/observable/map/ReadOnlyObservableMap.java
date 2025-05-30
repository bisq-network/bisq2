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

package bisq.common.observable.map;

import bisq.common.observable.Pin;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ReadOnlyObservableMap<K, V> {
    Map<K, V> getUnmodifiableMap();

    Pin addObserver(HashMapObserver<K, V> observer);

    Pin addObserver(Runnable observer);

    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    Set<K> keySet();

    Collection<V> values();

    Set<Map.Entry<K, V>> entrySet();
}
