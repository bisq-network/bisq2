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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

public class ObservableSet<T> extends CopyOnWriteArraySet<T> {
    // Must be a list, not a set as otherwise if 2 instances of the same component is using it, one would get replaced.
    private final List<Observer<T>> observers = new CopyOnWriteArrayList<>();

    public ObservableSet() {
    }

    public ObservableSet(Collection<T> values) {
        addAll(values);
    }

    private List<Observer<T>> getObservers() {
        return observers;
    }

    public Pin addChangedListener(Runnable handler) {
        ChangeListener<T> changedListener = new ChangeListener<>(handler);
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