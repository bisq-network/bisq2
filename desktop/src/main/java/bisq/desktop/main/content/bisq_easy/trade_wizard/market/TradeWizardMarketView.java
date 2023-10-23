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

package bisq.desktop.main.content.bisq_easy.trade_wizard.market;

import bisq.common.currency.FiatCurrency;
import bisq.common.currency.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class TradeWizardMarketView extends View<VBox, TradeWizardMarketModel, TradeWizardMarketController> {
    private final BisqTableView<MarketListItem> tableView;
    private final SearchBox searchBox;
    private final Label headlineLabel;

    public TradeWizardMarketView(TradeWizardMarketModel model, TradeWizardMarketController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.createOffer.market.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        searchBox = new SearchBox();
        searchBox.setMinWidth(140);
        searchBox.setMaxWidth(140);

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("bisq-easy-trade-wizard-market");
        double tableHeight = 290;
        int tableWidth = 650;
        tableView.setMinHeight(tableHeight);
        tableView.setMaxHeight(tableHeight);
        tableView.setMinWidth(tableWidth);
        tableView.setMaxWidth(tableWidth);
        configTableView();

        StackPane.setMargin(searchBox, new Insets(5, 15, 0, 0));
        StackPane tableViewWithSearchBox = new StackPane(tableView, searchBox);
        tableViewWithSearchBox.setAlignment(Pos.TOP_RIGHT);
        tableViewWithSearchBox.setPrefSize(tableWidth, tableHeight);
        tableViewWithSearchBox.setMaxWidth(tableWidth);

        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, subtitleLabel, tableViewWithSearchBox, Spacer.fillVBox());
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());
        tableView.getSelectionModel().select(model.getSelectedMarketListItem().get());

        searchBox.textProperty().bindBidirectional(model.getSearchText());

        // We use setOnMouseClicked handler not a listener on 
        // tableView.getSelectionModel().getSelectedItem() to get triggered the handler only at user action and 
        // not when we set the selected item by code.
        tableView.setOnMouseClicked(e -> controller.onMarketListItemClicked(tableView.getSelectionModel().getSelectedItem()));

        UIThread.runOnNextRenderFrame(() -> tableView.scrollTo(model.getSelectedMarketListItem().get()));
    }

    @Override
    protected void onViewDetached() {
        searchBox.textProperty().unbindBidirectional(model.getSearchText());
        tableView.setOnMouseClicked(null);
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("bisqEasy.createOffer.market.columns.name"))
                .left()
                .minWidth(150)
                .comparator(Comparator.comparing(MarketListItem::getQuoteCurrencyName))
                .setCellFactory(getNameCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("bisqEasy.createOffer.market.columns.numOffers"))
                .minWidth(60)
                .valueSupplier(MarketListItem::getNumOffers)
                .comparator(Comparator.comparing(MarketListItem::getNumOffersAsInteger))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("bisqEasy.createOffer.market.columns.numPeers"))
                .minWidth(60)
                .valueSupplier(MarketListItem::getNumUsers)
                .comparator(Comparator.comparing(MarketListItem::getNumUsersAsInteger))
                .build());
        // We add a placeholder column as we show the search field at the header which would hide the column header
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .fixWidth(140)
                .build());
    }

    private Callback<TableColumn<MarketListItem, MarketListItem>, TableCell<MarketListItem, MarketListItem>> getNameCellFactory
            () {
        return column -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setPadding(new Insets(0, 0, 0, -10));
                label.setGraphicTextGap(8);
                label.getStyleClass().add("bisq-text-8");
            }

            @Override
            public void updateItem(final MarketListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setGraphic(item.getIcon());
                    String quoteCurrencyName = item.getQuoteCurrencyName();
                    label.setText(quoteCurrencyName);
                    if (quoteCurrencyName.length() > 20) {
                        Tooltip tooltip = new BisqTooltip(quoteCurrencyName);
                        // Force font color as color from css gets shadowed by parent
                        tooltip.setStyle("-fx-text-fill: -fx-dark-text-color;");
                        label.setTooltip(tooltip);
                    }

                    setGraphic(label);
                } else {
                    label.setGraphic(null);
                    setGraphic(null);
                }
            }
        };
    }

    @EqualsAndHashCode
    @Getter
    static class MarketListItem implements TableItem {
        private final Market market;
        private final String quoteCurrencyName;
        private final int numOffersAsInteger;
        private final int numUsersAsInteger;
        private final String numOffers;
        private final String numUsers;
        @EqualsAndHashCode.Exclude
        private final StackPane icon;

        MarketListItem(Market market, int numOffersAsInteger, int numUsersAsInteger) {
            this.market = market;
            quoteCurrencyName = new FiatCurrency(market.getQuoteCurrencyCode()).getCodeAndName();
            this.numOffers = String.valueOf(numOffersAsInteger);
            this.numOffersAsInteger = numOffersAsInteger;
            this.numUsers = String.valueOf(numUsersAsInteger);
            icon = MarketImageComposition.getCurrencyIcon(market.getQuoteCurrencyCode());
            this.numUsersAsInteger = numUsersAsInteger;
        }

        @Override
        public String toString() {
            return market.toString();
        }
    }
}
