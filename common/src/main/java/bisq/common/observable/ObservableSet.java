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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ObservableSet<T> extends CopyOnWriteArraySet<T> {
    private record Observer<M, L>(Collection<L> collection, 
                                  Function<M, L> mapFunction, 
                                  Consumer<Runnable> executor) {

        public void add(M element) {
            executor.accept(() -> collection.add(mapFunction.apply(element)));
        }

        public void addAll(Collection<? extends M> values) {
            executor.accept(() -> collection.addAll(values.stream()
                    .map(mapFunction)
                    .collect(Collectors.toSet())));
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

    private final Set<Observer<T, ?>> observers = new CopyOnWriteArraySet<>();

    public ObservableSet() {
    }

    public ObservableSet(Collection<T> values) {
        addAll(values);
    }

    public <L> Pin addObserver(Collection<L> collection, Function<T, L> mapFunction, Consumer<Runnable> executor) {
        Observer<T, L> observer = new Observer<>(collection, mapFunction, executor);
        observer.clear();
        observer.addAll(this);
        observers.add(observer);
        return () -> observers.remove(observer);
    }

    @Override
    public boolean add(T element) {
        boolean result = super.add(element);
        if (result) {
            observers.forEach(observer -> observer.add(element));
        }
        return result;
    }

    public boolean addAll(Collection<? extends T> values) {
        boolean result = super.addAll(values);
        if (result) {
            observers.forEach(observer -> observer.addAll(values));
        }
        return result;
    }

    @Override
    public boolean remove(Object element) {
        boolean result = super.remove(element);
        if (result) {
            observers.forEach(observer -> observer.remove(element));
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> values) {
        boolean result = super.removeAll(values);
        if (result) {
            observers.forEach(observer -> observer.removeAll(values));
        }
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        observers.forEach(Observer::clear);
    }
}