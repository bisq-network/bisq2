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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;

/**
 * Simple Observer which notifies the given handler about any change of the target collection.
 *
 * @param <S> The type of the source collection element.
 */
@EqualsAndHashCode
@ToString
final class InvalidationHandler<S> implements Observer<S> {
    private final Runnable listener;

    public InvalidationHandler(Runnable listener) {
        this.listener = listener;
    }

    @Override
    public void add(S element) {
        listener.run();
    }

    @Override
    public void addAll(Collection<? extends S> values) {
        listener.run();
    }

    @Override
    public void remove(Object element) {
        listener.run();
    }

    @Override
    public void removeAll(Collection<?> values) {
        listener.run();
    }

    @Override
    public void clear() {
        listener.run();
    }
}