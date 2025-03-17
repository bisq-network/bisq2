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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ObservableArray<S> extends ObservableCollection<S> implements List<S> {
    public ObservableArray() {
        super();
    }

    public ObservableArray(Collection<S> values) {
        super(values);
    }

    @Override
    protected Collection<S> createCollection() {
        return new CopyOnWriteArrayList<>();
    }

    public List<S> getList() {
        return (List<S>) collection;
    }


    /* --------------------------------------------------------------------- */
    // List implementation

    /* --------------------------------------------------------------------- */

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends S> values) {
        boolean result = getList().addAll(index, values);
        if (result) {
            observers.forEach(observer -> {
                try {
                    observer.addAll(values);
                } catch (Exception e) {
                    log.error("Observer {} caused an exception at handling update.", observer, e);
                }
            });
        }
        return result;
    }

    @Override
    public S set(int index, S element) {
        S previous = getList().set(index, element);
        observers.forEach(observer -> {
            try {
                observer.add(element);
            } catch (Exception e) {
                log.error("Observer {} caused an exception at handling update.", observer, e);
            }
        });
        return previous;
    }

    @Override
    public void add(int index, S element) {
        getList().add(index, element);
        observers.forEach(observer -> {
            try {
                observer.add(element);
            } catch (Exception e) {
                log.error("Observer {} caused an exception at handling update.", observer, e);
            }
        });
    }

    @Override
    public S remove(int index) {
        S removedElement = getList().remove(index);
        observers.forEach(observer -> {
            try {
                observer.remove(removedElement);
            } catch (Exception e) {
                log.error("Observer {} caused an exception at handling update.", observer, e);
            }
        });
        return removedElement;
    }

    @Override
    public S get(int index) {
        return getList().get(index);
    }

    @Override
    public int indexOf(Object o) {
        return getList().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return getList().lastIndexOf(o);
    }

    @Override
    public ListIterator<S> listIterator() {
        return getList().listIterator();
    }

    @Override
    public ListIterator<S> listIterator(int index) {
        return getList().listIterator(index);
    }

    @Override
    public List<S> subList(int fromIndex, int toIndex) {
        return getList().subList(fromIndex, toIndex);
    }

    @Override
    public void sort(Comparator<? super S> c) {
        getList().sort(c);
    }
}