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

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Map Observer which notifies the listener about any change of the source map.
 *
 * @param <K, V> The map types.
 */
@EqualsAndHashCode
@ToString
final class HashMapChangeHandler<K, V> implements HashMapObserver<K, V> {
    private final Runnable listener;

    public HashMapChangeHandler(Runnable listener) {
        this.listener = listener;
    }

    void onChange() {
        listener.run();
    }

    @Override
    public void put(K key, V value) {
        onChange();
    }

    @Override
    public void remove(Object key) {
        onChange();
    }

    @Override
    public void clear() {
        onChange();
    }
}