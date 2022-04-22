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

package bisq.desktop.primary.main.content.multiSig.offerbook;

import bisq.desktop.common.view.TabViewChild;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OfferbookView extends View<VBox, OfferbookModel, OfferbookController> implements TabViewChild {
    private final BisqTableView<OfferListItem> tableView;
    private final ToggleButton showAllMarkets;
    private final Pane marketSelection;
    private final Button createOfferButton;

    public OfferbookView(OfferbookModel model,
                         OfferbookController controller,
                         Pane marketSelection,
                         Pane directionSelection) {
        super(new VBox(), model, controller);

        this.marketSelection = marketSelection;
        marketSelection.setMinWidth(280);

        root.setSpacing(30);
         root.setPadding(new Insets(0, 30, 0, 0));

        showAllMarkets = new ToggleButton();
        showAllMarkets.setText(Res.get("offerbook.showAllMarkets"));
        // showAllMarkets.setPadding(new Insets(6, 0, 0, 0));
        HBox.setMargin(showAllMarkets, new Insets(-6, 0, 0, 0));

        createOfferButton = new Button(Res.get("createOffer.createOffer.button"));
        createOfferButton.setDefaultButton(true);
        HBox.setMargin(createOfferButton, new Insets(-5, 0, 0, 0));

        Label headline = new Label(Res.get("offerbook.headline"));
        headline.getStyleClass().add("titled-group-bg-label-active");

        tableView = new BisqTableView<>(model.getSortedItems());
        tableView.setMinHeight(200);
        tableView.setPadding(new Insets(-20, 0, 0, 0));
        configDataTableView();

        HBox hBox = Layout.hBoxWith(marketSelection, showAllMarkets, Spacer.fillHBox(), createOfferButton);
        hBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(hBox,
                Layout.hBoxWith(directionSelection),
                headline,
                tableView);
    }

    @Override
    protected void onViewAttached() {
        showAllMarkets.setOnAction(e -> controller.onShowAllMarketsChanged(showAllMarkets.isSelected()));
        marketSelection.disableProperty().bind(model.marketSelectionDisabled);
        createOfferButton.setOnAction(e -> controller.onOpenCreateOffer());
        root.requestFocus();
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
                .isFirst()
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
                .fixWidth(200)
                .title(Res.get("offerbook.table.header.settlement"))
                .valueSupplier(model::getActionButtonTitle)
                .cellFactory(BisqTableColumn.DefaultCellFactories.BUTTON)
                .buttonClass(BisqIconButton.class)
                .updateItemWithButtonHandler(controller::onUpdateItemWithButton)
                .actionHandler(controller::onActionButtonClicked)
                .isLast()
                .build());
    }
}
