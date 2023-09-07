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
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

@Slf4j
public class BisqTableView<S extends TableItem> extends TableView<S> {
    private final static double TABLE_HEADER_HEIGHT = 36;
    private final static double TABLE_ROW_HEIGHT = 54;
    private final static double TABLE_SCROLLBAR_HEIGHT = 16;

    public BisqTableView() {
        super();

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        //setPlaceholder(new Label(Res.get("data.noDataAvailable")));
    }

    public BisqTableView(ObservableList<S> list) {
        this(new SortedList<>(list));
    }

    public BisqTableView(SortedList<S> sortedList) {
        super(sortedList);

        sortedList.comparatorProperty().bind(comparatorProperty());

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        //setPlaceholder(new Label(Res.get("data.noDataAvailable")));
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
        // As we adjust height we do not need the vertical scrollbar. 
        getStyleClass().add("hide-vertical-scrollbar");

        getItems().addListener(new WeakReference<>((ListChangeListener<S>) c ->
                adjustHeight(scrollbarHeight, headerHeight, rowHeight)).get());
        widthProperty().addListener(new WeakReference<>((ChangeListener<Number>) (observable, oldValue, newValue) ->
                UIThread.runOnNextRenderFrame(() -> adjustHeight(scrollbarHeight, headerHeight, rowHeight))).get());
        adjustHeight(scrollbarHeight, headerHeight, rowHeight);
    }

    private void adjustHeight(double scrollbarHeight, double headerHeight, double rowHeight) {
        double realScrollbarHeight = TableViewUtil.findScrollbar(BisqTableView.this, Orientation.HORIZONTAL)
                .map(e -> scrollbarHeight)
                .orElse(0d);
        double height = headerHeight + getItems().size() * rowHeight + realScrollbarHeight;
        setMinHeight(height);
        setMaxHeight(height);
    }
}