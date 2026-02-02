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

package bisq.common.observable.collection;

import java.util.Collection;

public interface CollectionObserver<S> {
    void onAdded(S element);

    default void onAllAdded(Collection<? extends S> values) {
        values.forEach(this::onAdded);
    }

    default void onAllSet(Collection<? extends S> values) {
        onCleared();
        onAllAdded(values);
    }

    void onRemoved(Object element);

    default void onAllRemoved(Collection<?> values) {
        values.forEach(this::onRemoved);
    }

    void onCleared();
}