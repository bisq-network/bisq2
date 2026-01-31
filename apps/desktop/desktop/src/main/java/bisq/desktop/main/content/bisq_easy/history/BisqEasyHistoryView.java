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

package bisq.desktop.main.content.bisq_easy.history;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

public class BisqEasyHistoryView extends View<VBox, BisqEasyHistoryModel, BisqEasyHistoryController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<BisqEasyTradeHistoryListItem> bisqEasyTradeHistoryListView;

    public BisqEasyHistoryView(BisqEasyHistoryModel model, BisqEasyHistoryController controller) {
        super(new VBox(), model, controller);

        bisqEasyTradeHistoryListView = new RichTableView<>(
                model.getSortedBisqEasyTradeHistoryListItems(),
                Res.get("bisqEasy.history.headline"),
                Res.get("bisqEasy.history.numTrades"),
                controller::applySearchPredicate);

        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getChildren().addAll(bisqEasyTradeHistoryListView);
    }

    @Override
    protected void onViewAttached() {

    }

    @Override
    protected void onViewDetached() {

    }
}
