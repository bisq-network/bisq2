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

import bisq.common.observable.Pin;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

@EqualsAndHashCode
public abstract class ObservableCollection<S> implements Collection<S> {
    protected final Collection<S> collection = createCollection();

    // Must be a list, not a set as otherwise if 2 instances of the same component is using it, one would get replaced.
    @EqualsAndHashCode.Exclude
    protected final List<CollectionObserver<S>> observers = new CopyOnWriteArrayList<>();

    protected ObservableCollection() {
    }

    protected ObservableCollection(Collection<S> values) {
        addAll(values);
    }

    protected abstract Collection<S> createCollection();

    public Pin addObserver(CollectionObserver<S> observer) {
        observers.add(observer);
        observer.addAll(collection);
        return () -> observers.remove(observer);
    }

    public Pin addObserver(Runnable observer) {
        SimpleCollectionObserver<S> simpleCollectionObserver = new SimpleCollectionObserver<>(observer);
        observers.add(simpleCollectionObserver);
        simpleCollectionObserver.onChange();
        return () -> observers.remove(simpleCollectionObserver);
    }

    public <T> Pin addCollectionChangeMapper(Collection<T> collection,
                                             Function<S, Boolean> filterFunction,
                                             Function<S, T> mapFunction,
                                             Consumer<Runnable> executor) {
        CollectionChangeMapper<S, T> collectionChangeMapper = new CollectionChangeMapper<>(collection, filterFunction, mapFunction, executor);
        collectionChangeMapper.clear();
        collectionChangeMapper.addAll(this);
        observers.add(collectionChangeMapper);
        return () -> observers.remove(collectionChangeMapper);
    }

    @Override
    public boolean add(S element) {
        boolean changed = collection.add(element);
        if (changed) {
            observers.forEach(observer -> observer.add(element));
        }
        return changed;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends S> values) {
        boolean changed = collection.addAll(values);
        if (changed) {
            observers.forEach(observer -> observer.addAll(values));
        }
        return changed;
    }

    public void setAll(@NotNull Collection<? extends S> values) {
        collection.clear();
        collection.addAll(values);
        observers.forEach(observer -> observer.setAll(values));
    }

    @Override
    public boolean remove(Object element) {
        boolean changed = collection.remove(element);
        if (changed) {
            observers.forEach(observer -> observer.remove(element));
        }
        return changed;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> values) {
        boolean changed = collection.removeAll(values);
        if (changed) {
            observers.forEach(observer -> observer.removeAll(values));
        }
        return changed;
    }

    @Override
    public void clear() {
        collection.clear();
        observers.forEach(CollectionObserver::clear);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("retainAll method is not implemented");
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return collection.containsAll(c);
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }

    @Override
    public Iterator<S> iterator() {
        return collection.iterator();
    }

    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @Override
    public <S1> S1[] toArray(@NotNull S1[] a) {
        return collection.toArray(a);
    }

    @Override
    public String toString() {
        return collection.toString();
    }
}