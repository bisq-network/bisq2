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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list.OfferbookListItem;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class ProfileCardOffersView extends View<VBox, ProfileCardOffersModel, ProfileCardOffersController> {
    private final BisqTableView<OfferbookListItem> tableView;

    public ProfileCardOffersView(ProfileCardOffersModel model,
                                 ProfileCardOffersController controller) {
        super(new VBox(), model, controller);

        VBox vBox = new VBox();
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("header");
        tableView = new BisqTableView<>(model.getListItems());
        tableView.getStyleClass().addAll("reputation-table", "rich-table-view");
        tableView.allowVerticalScrollbar();
        configTableView();
        root.getChildren().addAll(vBox, tableView);
        root.setPadding(new Insets(20, 0, 0, 0));
        root.getStyleClass().add("reputation");
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("user.profileCard.offers.table.columns.market"))
                .left()
                .comparator(Comparator.comparing(OfferbookListItem::getMarketCurrencyCode))
                .setCellFactory(getMarketCellFactory())
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("user.profileCard.offers.table.columns.offerType"))
                .left()
                .comparator(Comparator.comparing(OfferbookListItem::getOfferType))
                .valueSupplier(OfferbookListItem::getOfferType)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("user.profileCard.offers.table.columns.amount"))
                .left()
                .comparator(Comparator.comparing(OfferbookListItem::getQuoteSideMinAmount))
                .valueSupplier(OfferbookListItem::getFormattedRangeQuoteAmount)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("user.profileCard.offers.table.columns.price"))
                .right()
                .comparator(Comparator.comparing(OfferbookListItem::getPriceSpecAsPercent))
                .setCellFactory(getPriceCellFactory())
                .build());
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getMarketCellFactory() {
        return column -> new TableCell<>() {
            private final HBox marketLogoAndCodeBox = new HBox(10);
            private final Label marketCodeLabel = new Label();

            {
                marketLogoAndCodeBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    Node marketLogo = MarketImageComposition.createMarketLogo(item.getMarketCurrencyCode());
                    marketLogo.setCache(true);
                    marketLogo.setCacheHint(CacheHint.SPEED);
                    marketCodeLabel.setText(item.getMarketCurrencyCode());
                    marketLogoAndCodeBox.getChildren().setAll(marketLogo, marketCodeLabel);
                    setGraphic(marketLogoAndCodeBox);
                } else {
                    marketCodeLabel.setText("");
                    marketLogoAndCodeBox.getChildren().clear();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getPriceCellFactory() {
        return column -> new TableCell<>() {
            private final Label percentagePriceLabel = new Label();
            private final BisqTooltip tooltip = new BisqTooltip();

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    tooltip.setText(item.getPriceTooltipText());
                    percentagePriceLabel.setText(item.getFormattedPercentagePrice());
                    percentagePriceLabel.setTooltip(tooltip);
                    setGraphic(percentagePriceLabel);
                } else {
                    percentagePriceLabel.setText("");
                    percentagePriceLabel.setTooltip(null);
                    setGraphic(null);
                }
            }
        };
    }
}
