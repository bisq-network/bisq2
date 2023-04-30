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

@EqualsAndHashCode
@ToString
final class ObservableListMapper<M, L> implements Observer<M> {
    private final Collection<L> collection;
    private final Function<M, L> mapFunction;
    private final Consumer<Runnable> executor;

    ObservableListMapper(Collection<L> collection,
                         Function<M, L> mapFunction,
                         Consumer<Runnable> executor) {
        this.collection = collection;
        this.mapFunction = mapFunction;
        this.executor = executor;
    }

    public void add(M element) {
        executor.accept(() -> {
            L item = mapFunction.apply(element);
            if (!collection.contains(item)) {
                collection.add(item);
            }
        });
    }

    public void addAll(Collection<? extends M> values) {
        executor.accept(() -> values.forEach(element -> {
            L item = mapFunction.apply(element);
            if (!collection.contains(item)) {
                collection.add(item);
            }
        }));
    }

    public void remove(Object element) {
        //noinspection unchecked
        executor.accept(() -> collection.remove(mapFunction.apply((M) element)));
    }

    public void removeAll(Collection<?> values) {
        //noinspection unchecked
        executor.accept(() -> collection.removeAll(values.stream()
                .map(element -> mapFunction.apply((M) element))
                .collect(Collectors.toSet())));
    }

    public void clear() {
        executor.accept(collection::clear);
    }
}