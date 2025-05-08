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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A thread-safe observable map based on {@link java.util.concurrent.ConcurrentHashMap}.
 * <p>
 * Observers can register to be notified when entries are added, removed, or cleared.
 * <p>
 * All core map operations (`put`, `remove`, `clear`) trigger notifications to all registered {@link HashMapObserver}s.
 * Iteration over keys/values/entries is weakly consistent and does not throw {@link java.util.ConcurrentModificationException}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
@EqualsAndHashCode
public class ObservableHashMap<K, V> implements Map<K, V> {
    @Getter
    private final Map<K, V> map = new ConcurrentHashMap<>();

    @EqualsAndHashCode.Exclude
    private final List<HashMapObserver<K, V>> observers = new CopyOnWriteArrayList<>();

    public ObservableHashMap() {
    }

    private ObservableHashMap(Map<K, V> map) {
        putAll(map);
    }

    public Pin addObserver(HashMapObserver<K, V> observer) {
        observers.add(observer);
        observer.putAll(map);
        return () -> observers.remove(observer);
    }

    public Pin addObserver(Runnable observer) {
        SimpleHashMapObserver<K, V> simpleHashMapObserver = new SimpleHashMapObserver<>(observer);
        observers.add(simpleHashMapObserver);
        simpleHashMapObserver.onChange();
        return () -> observers.remove(simpleHashMapObserver);
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V result = map.put(key, value);
        observers.forEach(observer -> observer.put(key, value));
        return result;
    }

    @Override
    public V remove(Object key) {
        V result = map.remove(key);
        if (result != null) {
            observers.forEach(observer -> observer.remove(key));
        }
        return result;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        map.putAll(m);
        observers.forEach(observer -> observer.putAll(m));
    }

    @Override
    public void clear() {
        map.clear();
        observers.forEach(HashMapObserver::clear);
    }

    // Getters
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}