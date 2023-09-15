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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.TableViewUtil;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqTableView<S extends TableItem> extends TableView<S> {
    private final static double TABLE_HEADER_HEIGHT = 36;
    private final static double TABLE_ROW_HEIGHT = 54;
    private final static double TABLE_SCROLLBAR_HEIGHT = 16;
    private ListChangeListener<S> listChangeListener;
    private ChangeListener<Number> widthChangeListener;

    public BisqTableView() {
        super();

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public BisqTableView(ObservableList<S> list) {
        this(new SortedList<>(list));
    }

    public BisqTableView(SortedList<S> sortedList) {
        super(sortedList);

        sortedList.comparatorProperty().bind(comparatorProperty());
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void setPlaceholderText(String placeHolderText) {
        setPlaceholder(new Label(placeHolderText));
    }

    public void setFixHeight(double value) {
        setMinHeight(value);
        setMaxHeight(value);
    }

    public void adjustHeightToNumRows() {
        adjustHeightToNumRows(TABLE_SCROLLBAR_HEIGHT,
                TABLE_HEADER_HEIGHT,
                TABLE_ROW_HEIGHT);
    }

    public void adjustHeightToNumRows(double scrollbarHeight,
                                      double headerHeight,
                                      double rowHeight) {
        removeListeners();
        listChangeListener = c -> {
            adjustHeight(scrollbarHeight, headerHeight, rowHeight);
            UIThread.runOnNextRenderFrame(() -> adjustHeight(scrollbarHeight, headerHeight, rowHeight));
        };
        getItems().addListener(listChangeListener);

        widthChangeListener = (observable, oldValue, newValue) -> {
            adjustHeight(scrollbarHeight, headerHeight, rowHeight);
            UIThread.runOnNextRenderFrame(() -> adjustHeight(scrollbarHeight, headerHeight, rowHeight));
        };
        widthProperty().addListener(widthChangeListener);

        adjustHeight(scrollbarHeight, headerHeight, rowHeight);
    }

    public void hideVerticalScrollbar() {
        // As we adjust height we do not need the vertical scrollbar. 
        getStyleClass().add("hide-vertical-scrollbar");
    }

    public void allowVerticalScrollbar() {
        getStyleClass().remove("hide-vertical-scrollbar");
    }

    public void removeListeners() {
        if (listChangeListener != null) {
            getItems().removeListener(listChangeListener);
            listChangeListener = null;
        }
        if (widthChangeListener != null) {
            widthProperty().removeListener(widthChangeListener);
            widthChangeListener = null;
        }
    }

    private void adjustHeight(double scrollbarHeight, double headerHeight, double rowHeight) {
        int numItems = getItems().size();
        if (numItems == 0) {
            return;
        }
        double realScrollbarHeight = TableViewUtil.findScrollbar(BisqTableView.this, Orientation.HORIZONTAL)
                .map(e -> scrollbarHeight)
                .orElse(0d);
        double height = headerHeight + numItems * rowHeight + realScrollbarHeight;
        setFixHeight(height);
    }

    public BisqTableColumn<S> getSelectionMarkerColumn() {
        return new BisqTableColumn.Builder<S>()
                .fixWidth(3)
                .setCellFactory(getSelectionMarkerCellFactory())
                .isSortable(false)
                .build();
    }

    public Callback<TableColumn<S, S>, TableCell<S, S>> getSelectionMarkerCellFactory
            () {
        return column -> new TableCell<>() {
            private Subscription selectedPin;

            @Override
            public void updateItem(final S item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    selectedPin = EasyBind.subscribe(BisqTableView.this.getSelectionModel().selectedItemProperty(),
                            selected -> {
                                setId(item.equals(selected) ? "selection-marker" : null);
                            });
                } else {
                    if (selectedPin != null) {
                        selectedPin.unsubscribe();
                        selectedPin = null;
                    }
                }
            }
        };
    }
}