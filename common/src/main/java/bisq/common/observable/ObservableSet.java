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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObservableSet<T> extends CopyOnWriteArraySet<T> {
    @EqualsAndHashCode
    @ToString
    private static final class Observer<M, L> {
        private final Collection<L> collection;
        private final Function<M, L> mapFunction;
        private final Consumer<Runnable> executor;

        private Observer(Collection<L> collection,
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
            executor.accept(() -> {
                values.forEach(element -> {
                    L item = mapFunction.apply(element);
                    if (!collection.contains(item)) {
                        collection.add(item);
                    }
                });
            });
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

    private transient Set<Observer<T, ?>> observers = new CopyOnWriteArraySet<>();

    public ObservableSet() {
    }

    public ObservableSet(Collection<T> values) {
        addAll(values);
    }

    private Set<Observer<T, ?>> getObservers() {
        if (observers == null) {
            observers = new CopyOnWriteArraySet<>();
        }
        return observers;
    }

    public <L> Pin addObserver(Collection<L> collection, Function<T, L> mapFunction, Consumer<Runnable> executor) {
        Observer<T, L> observer = new Observer<>(collection, mapFunction, executor);
        observer.clear();
        observer.addAll(this);
        getObservers().add(observer);
        return () -> getObservers().remove(observer);
    }

    @Override
    public boolean add(T element) {
        boolean result = super.add(element);
        if (result) {
            getObservers().forEach(observer -> observer.add(element));
        }
        return result;
    }

    public boolean addAll(Collection<? extends T> values) {
        boolean result = super.addAll(values);
        if (result) {
            getObservers().forEach(observer -> observer.addAll(values));
        }
        return result;
    }

    @Override
    public boolean remove(Object element) {
        boolean result = super.remove(element);
        if (result) {
            getObservers().forEach(observer -> observer.remove(element));
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        boolean result = super.removeAll(values);
        if (result) {
            getObservers().forEach(observer -> observer.removeAll(values));
        }
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        getObservers().forEach(Observer::clear);
    }
}