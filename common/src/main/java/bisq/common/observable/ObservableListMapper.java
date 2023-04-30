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

package bisq.common.observable;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Observer implementation which maps the changes of the source collection to the
 * target collection (usually a JavaFx ObservableCollection) using the mapFunction and the given
 * executor (usually runs the runnable on the JavaFX Application Thread).
 * This is useful for mirroring changes of the source collection on the target collection while supporting
 * the UI frameworks constraints and mapping to different element types (usually ListItems).
 *
 * @param <S> The type of the collection element of the source collection
 * @param <T> The type of the collection element of the target collection
 */
@EqualsAndHashCode
@ToString
final class ObservableListMapper<S, T> implements Observer<S> {
    private final Collection<T> targetCollection;
    private final Function<S, T> mapFunction;
    private final Consumer<Runnable> executor;

    ObservableListMapper(Collection<T> targetCollection,
                         Function<S, T> mapFunction,
                         Consumer<Runnable> executor) {
        this.targetCollection = targetCollection;
        this.mapFunction = mapFunction;
        this.executor = executor;
    }

    @Override
    public void add(S element) {
        executor.accept(() -> {
            T item = mapFunction.apply(element);
            if (!targetCollection.contains(item)) {
                targetCollection.add(item);
            }
        });
    }

    @Override
    public void addAll(Collection<? extends S> values) {
        executor.accept(() -> values.forEach(element -> {
            T item = mapFunction.apply(element);
            if (!targetCollection.contains(item)) {
                targetCollection.add(item);
            }
        }));
    }

    @Override
    public void remove(Object element) {
        //noinspection unchecked
        executor.accept(() -> targetCollection.remove(mapFunction.apply((S) element)));
    }

    @Override
    public void removeAll(Collection<?> values) {
        //noinspection unchecked
        executor.accept(() -> targetCollection.removeAll(values.stream()
                .map(element -> mapFunction.apply((S) element))
                .collect(Collectors.toSet())));
    }

    @Override
    public void clear() {
        executor.accept(targetCollection::clear);
    }
}