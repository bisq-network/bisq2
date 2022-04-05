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

import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;

public class BisqTableView<S extends TableItem> extends TableView<S> {
    public BisqTableView() {
        super();

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new BisqLabel(Res.get("table.placeholder.noData")));
    }

    public BisqTableView(SortedList<S> sortedList) {
        super(sortedList);

        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sortedList.comparatorProperty().bind(comparatorProperty());

        setPlaceholder(new BisqLabel(Res.get("table.placeholder.noData")));
    }

    public void setFixHeight(double value) {
        setMinHeight(value);
        setMaxHeight(value);
    }
}