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

package bisq.common.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class SimpleObservableList<T> extends ArrayList<T> {

    private List<List<T>> boundLists = new CopyOnWriteArrayList<>();

    public void bindList(List<T> list) {
        boundLists.add(list);
    }

    @Override
    public T set(int index, T element) {
        T result = super.set(index, element);
        boundLists.forEach(l -> l.set(index, element));
        return result;
    }

    @Override
    public boolean add(T e) {
        boolean result = super.add(e);
        boundLists.forEach(l -> l.add(e));
        return result;
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        boundLists.forEach(l -> l.add(index, element));
    }


    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean result = super.addAll(c);
        boundLists.forEach(l -> l.addAll(c));
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean result = super.addAll(index, c);
        boundLists.forEach(l -> l.addAll(index, c));
        return result;
    }

    @Override
    public boolean remove(Object e) {
        boolean result = super.remove(e);
        boundLists.forEach(l -> l.remove(e));
        return result;
    }

    @Override
    public T remove(int index) {
        T removed = super.remove(index);
        boundLists.forEach(l -> l.remove(index));
        return removed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = super.removeAll(c);
        boundLists.forEach(l -> l.removeAll(c));
        return result;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        boolean result = super.removeIf(filter);
        boundLists.forEach(l -> l.removeIf(filter));
        return result;
    }

    @Override
    public void clear() {
        super.clear();
        boundLists.forEach(List::clear);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        super.replaceAll(operator);
        boundLists.forEach(l -> l.replaceAll(operator));
    }

    @Override
    public void sort(Comparator<? super T> c) {
        super.sort(c);
        boundLists.forEach(l -> l.sort(c));
    }
}