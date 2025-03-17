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
final class CollectionChangeMapper<S, T> implements CollectionObserver<S> {
    private final Collection<T> targetCollection;
    private final Function<S, Boolean> filterFunction;
    private final Function<S, T> mapFunction;
    private final Consumer<Runnable> executor;

    CollectionChangeMapper(Collection<T> targetCollection,
                           Function<S, Boolean> filterFunction,
                           Function<S, T> mapFunction,
                           Consumer<Runnable> executor) {
        this.targetCollection = targetCollection;
        this.filterFunction = filterFunction;
        this.mapFunction = mapFunction;
        this.executor = executor;
    }

    @Override
    public void add(S sourceItem) {
        executor.accept(() -> {
            if (filterFunction.apply(sourceItem)) {
                T item = mapFunction.apply(sourceItem);
                if (!targetCollection.contains(item)) {
                    targetCollection.add(item);
                }
            }
        });
    }

    @Override
    public void addAll(Collection<? extends S> sourceItems) {
        executor.accept(() -> targetCollection.addAll(sourceItems.stream()
                .filter(filterFunction::apply)
                .map(mapFunction)
                .filter(item -> !targetCollection.contains(item))
                .toList()));
    }

    @Override
    public void setAll(Collection<? extends S> sourceItems) {
        executor.accept(() -> {
            targetCollection.clear();
            targetCollection.addAll(sourceItems.stream()
                    .filter(filterFunction::apply)
                    .map(mapFunction)
                    .toList());
        });
    }

    @Override
    public void remove(Object sourceItem) {
        executor.accept(() -> {
            //noinspection unchecked
            S sourceItemCasted = (S) sourceItem;
            // We do not apply the filter at remove as the remove action could have impact on the filter
            // predicate (e.g. if we close an item and set a flag used in the filter)
            T item = mapFunction.apply(sourceItemCasted);
            targetCollection.remove(item);
        });
    }

    @Override
    public void removeAll(Collection<?> sourceItems) {
        executor.accept(() -> targetCollection.removeAll(sourceItems.stream()
                .map(element -> {
                    //noinspection unchecked
                    return (S) element;
                })
                .map(mapFunction)
                .collect(Collectors.toSet())));
    }

    @Override
    public void clear() {
        executor.accept(targetCollection::clear);
    }
}