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

package bisq.desktop.primary.main.content.trade.multisig.old.closedTrades;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosedTradesView extends View<VBox, ClosedTradesModel, ClosedTradesController> {

    private final BisqTableView<ClosedTradeListItem> tableView;

    public ClosedTradesView(ClosedTradesModel model, ClosedTradesController controller) {
        super(new VBox(), model, controller);

        root.setPadding(new Insets(33, 0, 0, 0));
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
        tableView.getColumns().add(new BisqTableColumn.Builder<ClosedTradeListItem>()
                .title(Res.get("offerbook.table.header.market"))
                .minWidth(80)
                .valueSupplier(ClosedTradeListItem::getMarket)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ClosedTradeListItem>()
                .title(Res.get("openOffers.table.header.price"))
                .minWidth(120)
                .valueSupplier(ClosedTradeListItem::getPrice)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ClosedTradeListItem>()
                .title(Res.get("openOffers.table.header.baseAmount"))
                .minWidth(80)
                .valueSupplier(ClosedTradeListItem::getBaseAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ClosedTradeListItem>()
                .minWidth(80)
                .title(Res.get("openOffers.table.header.quoteAmount"))
                .valueSupplier(ClosedTradeListItem::getQuoteAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ClosedTradeListItem>()
                .minWidth(100)
                .title(Res.get("offerbook.table.header.settlement"))
                .valueSupplier(ClosedTradeListItem::getSettlement)
                .build());
    }
}
