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

package bisq.desktop.primary.main.content.trade.overview;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeOverviewView extends View<VBox, TradeOverviewModel, TradeOverviewController> {
    private static final int MARGIN = 44;
    private final BisqTableView<ProtocolListItem> tableView;


    public TradeOverviewView(TradeOverviewModel model, TradeOverviewController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(30);

        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
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
        BisqTableColumn<ProtocolListItem> column = new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Protocol type".toUpperCase())
                .minWidth(80)
                .valueSupplier(e -> e.getSwapProtocolType().name())
                .build();
       // column.setTitleWithHelpText("ddd","safsad");
        column.getStyleClass().add("first");
        tableView.getColumns().add(column);
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Security".toUpperCase())
                .minWidth(80)
                .valueSupplier(e -> e.getSwapProtocolType().getSecurity().name())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Privacy".toUpperCase())
                .minWidth(80)
                .valueSupplier(e -> e.getSwapProtocolType().getPrivacy().name())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Convenience".toUpperCase())
                .minWidth(80)
                .valueSupplier(e -> e.getSwapProtocolType().getConvenience().name())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Costs".toUpperCase())
                .minWidth(80)
                .valueSupplier(e -> e.getSwapProtocolType().getCost().name())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Speed".toUpperCase())
                .minWidth(80)
                .valueSupplier(e -> e.getSwapProtocolType().getSpeed().name())
                .build());
     
       column = new BisqTableColumn.Builder<ProtocolListItem>()
                .fixWidth(150)
                .value(Res.get("shared.select"))
                .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                .buttonClass(BisqIconButton.class)
                .actionHandler(controller::onSelect)
                .build();
        column.getStyleClass().add("last");
        tableView.getColumns().add(column);
      /*  tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .title("Security")
                .minWidth(80)
                .valueSupplier(ProtocolListItem::getMarket)
                .build());*/
      /*  tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .titleProperty(model.getPriceHeaderTitle())
                .minWidth(120)
                .valueSupplier(ProtocolListItem::getPrice)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .titleProperty(model.getBaseAmountHeaderTitle())
                .minWidth(80)
                .valueSupplier(ProtocolListItem::getBaseAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .minWidth(80)
                .titleProperty(model.getQuoteAmountHeaderTitle())
                .valueSupplier(ProtocolListItem::getQuoteAmount)
                .build());*/
      /*  tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .minWidth(100)
                .title(Res.get("offerbook.table.header.settlement"))
                .valueSupplier(ProtocolListItem::getSettlement)
                .build());*/
      /*  tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .minWidth(150)
                .title(Res.get("offerbook.table.header.options"))
                .valueSupplier(ProtocolListItem::getOptions)
                .build());*/
      /*  tableView.getColumns().add(new BisqTableColumn.Builder<ProtocolListItem>()
                .fixWidth(150)
                .value(Res.get("remove"))
                .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                .buttonClass(BisqIconButton.class)
                .actionHandler(controller::onRemoveOffer)
                .build());*/
    }
}
