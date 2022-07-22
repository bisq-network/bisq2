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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObservableSet<T> extends CopyOnWriteArraySet<T> {
    public interface Observer<M, L> {
        void add(M element);

        void addAll(Collection<? extends M> values);

        void remove(Object element);

        void removeAll(Collection<?> values);

        void clear();
    }

    @EqualsAndHashCode
    @ToString
    private static final class ChangeListener<M, L> implements Observer<M, L> {
        private final Runnable handler;

        public ChangeListener(Runnable handler) {
            this.handler = handler;
        }

        @Override
        public void add(M element) {
            handler.run();
        }

        @Override
        public void addAll(Collection<? extends M> values) {
            handler.run();
        }

        @Override
        public void remove(Object element) {
            handler.run();
        }

        @Override
        public void removeAll(Collection<?> values) {
            handler.run();
        }

        @Override
        public void clear() {
            handler.run();
        }
    }

    @EqualsAndHashCode
    @ToString
    private static final class ObservableListMapper<M, L> implements Observer<M, L> {
        private final Collection<L> collection;
        private final Function<M, L> mapFunction;
        private final Consumer<Runnable> executor;

        private ObservableListMapper(Collection<L> collection,
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

    // Must be a list, not a set as otherwise if 2 instances of the same component is using it, one would get replaced.
    private transient List<Observer<T, ?>> observableListMappers = new CopyOnWriteArrayList<>();

    public ObservableSet() {
    }

    public ObservableSet(Collection<T> values) {
        addAll(values);
    }

    private List<Observer<T, ?>> getObservers() {
        if (observableListMappers == null) {
            observableListMappers = new CopyOnWriteArrayList<>();
        }
        return observableListMappers;
    }

    public <L> Pin addChangedListener(Runnable handler) {
        ChangeListener<T, L> changedListener = new ChangeListener<>(handler);
        getObservers().add(changedListener);
        handler.run();
        return () -> getObservers().remove(changedListener);
    }

    public <L> Pin addObservableListMapper(Collection<L> collection, Function<T, L> mapFunction, Consumer<Runnable> executor) {
        ObservableListMapper<T, L> observableListMapper = new ObservableListMapper<>(collection, mapFunction, executor);
        observableListMapper.clear();
        observableListMapper.addAll(this);
        getObservers().add(observableListMapper);
        return () -> getObservers().remove(observableListMapper);
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