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

package bisq.desktop.primary.main.content.trade.offerbook;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferbookView extends View<VBox, OfferbookModel, OfferbookController> {
    private final BisqTableView<OfferListItem> tableView;

    public OfferbookView(OfferbookModel model, OfferbookController controller, MarketSelection.MarketSelectionView marketSelectionView) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 0));

        Label headline = new BisqLabel(Res.offerbook.get("offerbook.headline"));
        headline.getStyleClass().add("titled-group-bg-label-active");

        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
        configDataTableView();

        root.getChildren().addAll(marketSelectionView.getRoot(), headline, tableView);
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void configDataTableView() {
       /* var dateColumn = new BisqTableColumn.Builder<OfferListItem>()
                .title(Res.common.get("date"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(OfferListItem::getDateString)
                .comparator(OfferListItem::compareDate)
                .build();
        tableView.getColumns().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        tableView.getSortOrder().add(dateColumn);*/

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .title(Res.offerbook.get("offerbook.table.header.market"))
                .minWidth(120)
                .valueSupplier(OfferListItem::getMarket)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .titleProperty(model.getPriceHeaderTitle())
                .minWidth(120)
                .valueSupplier(OfferListItem::getPrice)
                .build());
        
        /*
        offerbook.table.header.price=Price in {0} for 1 {1}
offerbook.table.header.baseAmount={0} (min-max)
offerbook.table.header.quoteAmount={0} (min-max)
offerbook.table.header.settlement=Settlement method
offerbook.table.header.options=Options
offerbook.table.header.action=Actions
offerbook.table.header.maker=Maker
         */
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .titleProperty(model.getBaseAmountHeaderTitle())
                .minWidth(150)
                .valueSupplier(OfferListItem::getBaseAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(150)
                .titleProperty(model.getQuoteAmountHeaderTitle())
                .valueSupplier(OfferListItem::getQuoteAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(150)
                .title(Res.offerbook.get("offerbook.table.header.settlement"))
                .valueSupplier(OfferListItem::getSettlement)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(150)
                .title(Res.offerbook.get("offerbook.table.header.options"))
                .valueSupplier(OfferListItem::getOptions)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(80)
                .title(Res.offerbook.get("offerbook.table.header.action"))
                .valueSupplier(model::getActionButtonTitle)
                .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                .actionHandler(controller::onActionButtonClicked)
                .build());
    }
}
