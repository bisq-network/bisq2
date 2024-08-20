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

package bisq.desktop.components.table;

import bisq.desktop.common.threading.UIScheduler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BisqTableView<T> extends TableView<T> {
    public final static double TABLE_HEADER_HEIGHT = 36;
    public final static double TABLE_ROW_HEIGHT = 54;
    public final static double TABLE_H_SCROLLBAR_HEIGHT = 16;
    public final static double TABLE_V_SCROLLBAR_WIDTH = 16;

    @Getter
    private final SortedList<T> sortedList;
    private final boolean useComparatorBinding;
    // If set we use the sum of the minWidth values of all visible columns to set the minWidth of the tableView.
    @Setter
    private boolean deriveMinWidthFromColumns = true;

    @Getter
    private final ObjectProperty<ScrollBar> horizontalScrollbar = new SimpleObjectProperty<>();
    @Getter
    private final ObjectProperty<ScrollBar> verticalScrollbar = new SimpleObjectProperty<>();
    private Subscription verticalScrollbarVisiblePin;
    private Subscription verticalScrollbarPin;

    public BisqTableView(ObservableList<T> list) {
        this(new SortedList<>(list));
    }

    public BisqTableView(ObservableList<T> list, boolean useComparatorBinding) {
        this(new SortedList<>(list), useComparatorBinding);
    }

    public BisqTableView(SortedList<T> sortedList) {
        this(sortedList, true);
    }

    public BisqTableView(SortedList<T> sortedList, boolean useComparatorBinding) {
        super(sortedList);
        this.sortedList = sortedList;
        this.useComparatorBinding = useComparatorBinding;

        if (useComparatorBinding) {
            // Need to bind early as otherwise table not applying it
            sortedList.comparatorProperty().bind(this.comparatorProperty());
        }
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public void initialize() {
        if (useComparatorBinding) {
            sortedList.comparatorProperty().unbind();
            sortedList.comparatorProperty().bind(this.comparatorProperty());

            if (deriveMinWidthFromColumns) {
                verticalScrollbarPin = EasyBind.subscribe(verticalScrollbar, scrollBar -> {
                    if (scrollBar != null) {
                        verticalScrollbarVisiblePin = EasyBind.subscribe(scrollBar.visibleProperty(), scrollBarVisible -> {
                            if (scrollBarVisible != null) {
                                adjustMinWidth();
                            }
                        });
                    }
                });

                // We get verticalScrollbar property set if we find one after some delayed iterations
                recursiveFindScrollbar(this, Orientation.VERTICAL);
            }
        }
    }

    public void removeListeners() {
        if (verticalScrollbarPin != null) {
            verticalScrollbarPin.unsubscribe();
        }
        if (verticalScrollbarVisiblePin != null) {
            verticalScrollbarVisiblePin.unsubscribe();
        }
    }

    public void dispose() {
        removeListeners();

        if (useComparatorBinding) {
            sortedList.comparatorProperty().unbind();
        }
        horizontalScrollbar.set(null);
        verticalScrollbar.set(null);
    }

    public void setPlaceholderText(String placeHolderText) {
        setPlaceholder(new Label(placeHolderText));
    }

    public void setFixHeight(double value) {
        setMinHeight(value);
        setMaxHeight(value);
    }

    public double calculateTableHeight(int maxNumItems) {
        return calculateTableHeight(getItems().size(), maxNumItems, TABLE_H_SCROLLBAR_HEIGHT, TABLE_HEADER_HEIGHT, TABLE_ROW_HEIGHT);
    }

    public double calculateTableHeight(int numItems,
                                       int maxNumItems,
                                       double scrollbarHeight,
                                       double headerHeight,
                                       double rowHeight) {
        if (getItems().isEmpty()) {
            return 0;
        }
        int boundedNumItems = Math.min(maxNumItems, numItems);
        double realScrollbarHeight = findScrollbar(BisqTableView.this, Orientation.HORIZONTAL)
                .filter(Node::isVisible)
                .map(e -> scrollbarHeight)
                .orElse(0d);
        return headerHeight + boundedNumItems * rowHeight + realScrollbarHeight;
    }

    public void hideVerticalScrollbar() {
        // As we adjust height we do not need the vertical scrollbar.
        getStyleClass().add("hide-vertical-scrollbar");
    }

    public void allowVerticalScrollbar() {
        getStyleClass().remove("hide-vertical-scrollbar");
    }

    public void hideHorizontalScrollbar() {
        getStyleClass().add("force-hide-horizontal-scrollbar");
    }

    public void allowHorizontalScrollbar() {
        getStyleClass().remove("force-hide-horizontal-scrollbar");
    }

    public void adjustMinWidth() {
        double value = sumOfColumns() + scrollbarWidth();
        // FIXME not always triggering a layout update.
        setMinWidth(value);
    }

    private Double scrollbarWidth() {
        return Optional.ofNullable(verticalScrollbar.get())
                .filter(Node::isVisible)
                .map(s -> TABLE_V_SCROLLBAR_WIDTH)
                .orElse(0d);
    }

    private double sumOfColumns() {
        return getColumns().stream()
                .filter(TableColumnBase::isVisible)
                .mapToDouble(TableColumnBase::getMinWidth)
                .sum();
    }


    public BisqTableColumn<T> getSelectionMarkerColumn() {
        return new BisqTableColumn.Builder<T>()
                .fixWidth(3)
                .setCellFactory(getSelectionMarkerCellFactory())
                .isSortable(false)
                .build();
    }

    public Callback<TableColumn<T, T>, TableCell<T, T>> getSelectionMarkerCellFactory() {
        return column -> new TableCell<>() {
            private Subscription selectedPin;

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    TableRow<T> tableRow = getTableRow();
                    if (tableRow != null) {
                        if (selectedPin != null) {
                            selectedPin.unsubscribe();
                        }
                        selectedPin = EasyBind.subscribe(tableRow.selectedProperty(), isSelected ->
                                setId(isSelected ? "selection-marker" : null));
                    }
                } else {
                    if (selectedPin != null) {
                        selectedPin.unsubscribe();
                        selectedPin = null;
                    }
                }
            }
        };
    }

    public static void recursiveFindScrollbar(BisqTableView<?> tableView, Orientation orientation) {
        ObjectProperty<ScrollBar> scrollbar = orientation == Orientation.HORIZONTAL
                ? tableView.getHorizontalScrollbar()
                : tableView.getVerticalScrollbar();
        recursiveFindScrollbar(tableView, orientation, scrollbar, new AtomicInteger(0), 10);
    }

    public static void recursiveFindScrollbar(BisqTableView<?> tableView,
                                              Orientation orientation,
                                              ObjectProperty<ScrollBar> scrollBar,
                                              AtomicInteger numIterations,
                                              int maxIterations) {
        Optional<ScrollBar> candidate = findScrollbar(tableView, orientation);
        if (candidate.isPresent()) {
            scrollBar.set(candidate.get());
        } else if (numIterations.incrementAndGet() < maxIterations) {
            UIScheduler.run(() ->
                            recursiveFindScrollbar(tableView, orientation, scrollBar, numIterations, maxIterations))
                    .after(100);
        }
    }

    public static Optional<ScrollBar> findScrollbar(BisqTableView<?> tableView, Orientation orientation) {
        ObjectProperty<ScrollBar> cachedScrollbar = orientation == Orientation.HORIZONTAL
                ? tableView.getHorizontalScrollbar()
                : tableView.getVerticalScrollbar();
        if (cachedScrollbar.get() != null) {
            return Optional.of(cachedScrollbar.get());
        }

        Optional<ScrollBar> scrollbar = tableView.lookupAll(".scroll-bar").stream()
                .filter(node -> node instanceof ScrollBar)
                .map(node -> (ScrollBar) node)
                .filter(scrollBar -> scrollBar.getOrientation().equals(orientation))
                .filter(Node::isVisible)
                .findAny();
        scrollbar.ifPresent(cachedScrollbar::set);

        return scrollbar;
    }
}