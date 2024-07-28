package bisq.desktop.components.table;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.util.Subscription;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@Getter
public class TableList<E> implements ObservableList<E> {
    private final ObservableList<E> observableList = FXCollections.observableArrayList();
    private final FilteredList<E> filteredList = new FilteredList<>(observableList);
    private final SortedList<E> sortedList = new SortedList<>(filteredList);
    private final ListChangeListener<E> listChangeListener;

    public TableList() {
        listChangeListener = c -> {
            c.next();
            if (c.wasAdded()) {
                c.getAddedSubList().stream()
                        .filter(e -> e instanceof ActivatableTableItem)
                        .map(e -> (ActivatableTableItem) e)
                        .forEach(ActivatableTableItem::onActivate);
            } else if (c.wasRemoved()) {
                c.getRemoved().stream()
                        .filter(e -> e instanceof ActivatableTableItem)
                        .map(e -> (ActivatableTableItem) e)
                        .forEach(ActivatableTableItem::onDeactivate);
            }
        };
    }

    public void onActivate() {
        filteredList.stream()
                .filter(e -> e instanceof ActivatableTableItem)
                .map(e -> (ActivatableTableItem) e)
                .forEach(ActivatableTableItem::onActivate);
        filteredList.addListener(listChangeListener);
    }

    public void onDeactivate() {
        observableList.clear(); // triggers onDeactivate
        filteredList.removeListener(listChangeListener);
    }

    public void setPredicate(Predicate<? super E> predicate) {
        filteredList.setPredicate(predicate);
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    // Delegates for observableList (Lombok @Delegate did not work in the IDE with Java22)
    ////////////////////////////////////////////////////////////////////////////////////////

    public void addListener(ListChangeListener<? super E> listener) {
        this.observableList.addListener(listener);
    }

    public void removeListener(ListChangeListener<? super E> listener) {
        this.observableList.removeListener(listener);
    }

    @SuppressWarnings("unchecked")
    public boolean addAll(E... elements) {
        return this.observableList.addAll(elements);
    }

    @SuppressWarnings("unchecked")
    public boolean setAll(E... elements) {
        return this.observableList.setAll(elements);
    }

    public boolean setAll(Collection<? extends E> col) {
        return this.observableList.setAll(col);
    }

    @SuppressWarnings("unchecked")
    public boolean removeAll(E... elements) {
        return this.observableList.removeAll(elements);
    }

    @SuppressWarnings("unchecked")
    public boolean retainAll(E... elements) {
        return this.observableList.retainAll(elements);
    }

    public void remove(int from, int to) {
        this.observableList.remove(from, to);
    }

    public FilteredList<E> filtered(Predicate<E> predicate) {
        return this.observableList.filtered(predicate);
    }

    public SortedList<E> sorted(Comparator<E> comparator) {
        return this.observableList.sorted(comparator);
    }

    public SortedList<E> sorted() {
        return this.observableList.sorted();
    }

    public int size() {
        return this.observableList.size();
    }

    public boolean isEmpty() {
        return this.observableList.isEmpty();
    }

    public boolean contains(Object o) {
        return this.observableList.contains(o);
    }

    public Iterator<E> iterator() {
        return this.observableList.iterator();
    }

    public Object[] toArray() {
        return this.observableList.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.observableList.toArray(a);
    }

    public boolean add(E e) {
        return this.observableList.add(e);
    }

    public boolean remove(Object o) {
        return this.observableList.remove(o);
    }

    @SuppressWarnings("SlowListContainsAll")
    public boolean containsAll(Collection<?> c) {
        return this.observableList.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return this.observableList.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        return this.observableList.addAll(index, c);
    }

    public boolean removeAll(Collection<?> c) {
        return this.observableList.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return this.observableList.retainAll(c);
    }

    public void replaceAll(UnaryOperator<E> operator) {
        this.observableList.replaceAll(operator);
    }

    public void sort(Comparator<? super E> c) {
        this.observableList.sort(c);
    }

    public void clear() {
        this.observableList.clear();
    }

    public E get(int index) {
        return this.observableList.get(index);
    }

    public E set(int index, E element) {
        return this.observableList.set(index, element);
    }

    public void add(int index, E element) {
        this.observableList.add(index, element);
    }

    public E remove(int index) {
        return this.observableList.remove(index);
    }

    public int indexOf(Object o) {
        return this.observableList.indexOf(o);
    }

    public int lastIndexOf(Object o) {
        return this.observableList.lastIndexOf(o);
    }

    public ListIterator<E> listIterator() {
        return this.observableList.listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return this.observableList.listIterator(index);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return this.observableList.subList(fromIndex, toIndex);
    }

    public Spliterator<E> spliterator() {
        return this.observableList.spliterator();
    }

    public void addFirst(E e) {
        this.observableList.addFirst(e);
    }

    public void addLast(E e) {
        this.observableList.addLast(e);
    }

    public E getFirst() {
        return this.observableList.getFirst();
    }

    public E getLast() {
        return this.observableList.getLast();
    }

    public E removeFirst() {
        return this.observableList.removeFirst();
    }

    public E removeLast() {
        return this.observableList.removeLast();
    }

    public List<E> reversed() {
        return this.observableList.reversed();
    }

    public <T> T[] toArray(IntFunction<T[]> generator) {
        return this.observableList.toArray(generator);
    }

    public boolean removeIf(Predicate<? super E> filter) {
        return this.observableList.removeIf(filter);
    }

    public Stream<E> stream() {
        return this.observableList.stream();
    }

    public Stream<E> parallelStream() {
        return this.observableList.parallelStream();
    }

    public void forEach(Consumer<? super E> action) {
        this.observableList.forEach(action);
    }

    public void addListener(InvalidationListener listener) {
        this.observableList.addListener(listener);
    }

    public void removeListener(InvalidationListener listener) {
        this.observableList.removeListener(listener);
    }

    public Subscription subscribe(Runnable invalidationSubscriber) {
        return this.observableList.subscribe(invalidationSubscriber);
    }
}
