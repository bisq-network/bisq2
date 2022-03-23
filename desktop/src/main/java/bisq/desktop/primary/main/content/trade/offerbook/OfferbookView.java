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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqToggleButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferbookView extends View<VBox, OfferbookModel, OfferbookController> {
    private final BisqTableView<OfferListItem> tableView;
    private final BisqToggleButton showAllMarkets;
    private final BisqButton createOfferButton;
    private final Pane marketSelection;

    public OfferbookView(OfferbookModel model, OfferbookController controller,
                         Pane marketSelection,
                         Pane directionSelection) {
        super(new VBox(), model, controller);
        this.marketSelection = marketSelection;

        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 0));

        showAllMarkets = new BisqToggleButton();
        showAllMarkets.setText(Res.get("offerbook.showAllMarkets"));
        showAllMarkets.setPadding(new Insets(23, 0, -10, 0));

        createOfferButton = new BisqButton(Res.get("createOffer.createOffer.button"));
        createOfferButton.getStyleClass().add("action-button");
        createOfferButton.setFixWidth(200);
        HBox.setMargin(createOfferButton, new Insets(30, 19, 0, 0));

        Label headline = new BisqLabel(Res.get("offerbook.headline"));
        headline.getStyleClass().add("titled-group-bg-label-active");

        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
        tableView.setPadding(new Insets(-20, 0, 0, 0));
        configDataTableView();

        this.root.getChildren().addAll(Layout.hBoxWith(marketSelection, showAllMarkets),
                Layout.hBoxWith(directionSelection, Spacer.fillHBox(), createOfferButton),
                headline,
                tableView);
    }

    @Override
    public void onViewAttached() {
        showAllMarkets.setOnAction(e -> controller.onShowAllMarketsChanged(showAllMarkets.isSelected()));
        createOfferButton.setOnAction(e -> controller.onCreateOffer());
        marketSelection.disableProperty().bind(model.marketSelectionDisabled);
    }

    @Override
    protected void onViewDetached() {
        showAllMarkets.setOnAction(null);
        createOfferButton.setOnAction(null);
        marketSelection.disableProperty().unbind();
    }

    private void configDataTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .title(Res.get("offerbook.table.header.market"))
                .minWidth(80)
                .valueSupplier(OfferListItem::getMarket)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .titleProperty(model.getPriceHeaderTitle())
                .minWidth(120)
                .valueSupplier(OfferListItem::getPrice)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .titleProperty(model.getBaseAmountHeaderTitle())
                .minWidth(80)
                .valueSupplier(OfferListItem::getBaseAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(80)
                .titleProperty(model.getQuoteAmountHeaderTitle())
                .valueSupplier(OfferListItem::getQuoteAmount)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(100)
                .title(Res.get("offerbook.table.header.settlement"))
                .valueSupplier(OfferListItem::getSettlement)
                .build());
      /*  tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .minWidth(150)
                .title(Res.get("offerbook.table.header.options"))
                .valueSupplier(OfferListItem::getOptions)
                .build());*/
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferListItem>()
                .fixWidth(220)
                .title(Res.get("offerbook.table.header.action"))
                .valueSupplier(model::getActionButtonTitle)
                .cellFactory(BisqTableColumn.CellFactory.BUTTON)
                .buttonClass(BisqIconButton.class)
                .updateItemWithButtonHandler(controller::onUpdateItemWithButton)
                .actionHandler(controller::onActionButtonClicked)
                .build());
    }
}
