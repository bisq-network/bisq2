package bisq.desktop.components.table;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.function.Predicate;

@Getter
public class TableList<T> implements ObservableList<T> {
    @Delegate
    private final ObservableList<T> observableList = FXCollections.observableArrayList();
    private final FilteredList<T> filteredList = new FilteredList<>(observableList);
    private final SortedList<T> sortedList = new SortedList<>(filteredList);
    private final ListChangeListener<T> listChangeListener;

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

    public void setPredicate(Predicate<? super T> predicate) {
        filteredList.setPredicate(predicate);
    }
}
