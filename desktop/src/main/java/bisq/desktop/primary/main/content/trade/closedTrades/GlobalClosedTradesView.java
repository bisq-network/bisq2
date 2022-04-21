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

package bisq.desktop.primary.main.content.trade.closedTrades;

import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalClosedTradesView extends View<VBox, GlobalClosedTradesModel, GlobalClosedTradesController> implements TabViewChild {

    private final BisqTableView<GlobalClosedTradeListItem> tableView;

    public GlobalClosedTradesView(GlobalClosedTradesModel model, GlobalClosedTradesController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(30);

        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
        VBox.setMargin(tableView, new Insets(-33, 0, 0, 0));
        configDataTableView();

        this.root.getChildren().addAll(tableView);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void configDataTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<GlobalClosedTradeListItem>()
                .title(Res.get("offerbook.table.header.market"))
                .minWidth(80)
                .valueSupplier(GlobalClosedTradeListItem::getMarket)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<GlobalClosedTradeListItem>()
                .title(Res.get("openOffers.table.header.price"))
                .minWidth(120)
                .valueSupplier(GlobalClosedTradeListItem::getPrice)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<GlobalClosedTradeListItem>()
                .title(Res.get("openOffers.table.header.baseAmount"))
                .minWidth(80)
                .valueSupplier(GlobalClosedTradeListItem::getBaseAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<GlobalClosedTradeListItem>()
                .minWidth(80)
                .title(Res.get("openOffers.table.header.quoteAmount"))
                .valueSupplier(GlobalClosedTradeListItem::getQuoteAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<GlobalClosedTradeListItem>()
                .minWidth(100)
                .title(Res.get("offerbook.table.header.settlement"))
                .valueSupplier(GlobalClosedTradeListItem::getSettlement)
                .build());
    }
}
