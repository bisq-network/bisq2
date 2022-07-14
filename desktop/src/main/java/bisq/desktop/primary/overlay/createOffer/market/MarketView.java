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

package bisq.desktop.primary.overlay.createOffer.market;

import bisq.common.currency.Market;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqToggleButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.primary.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class MarketView extends View<VBox, MarketModel, MarketController> {
    private final BisqTableView<MarketListItem> tableView;
    private final TextField searchField;

    public MarketView(MarketModel model, MarketController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("onboarding.market.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.market.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        ImageView searchIcon = ImageUtil.getImageViewById("search-white");
        searchField = new TextField();
        searchField.setPromptText(Res.get("search"));
        searchField.getStyleClass().add("small-search-text");

        HBox.setMargin(searchIcon, new Insets(0, -3, 0, 7));
        HBox searchBox = new HBox(0, searchIcon, searchField);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        int searchBoxWidth = 158;
        searchBox.setMaxWidth(searchBoxWidth);
        searchBox.setMaxHeight(26);
        searchBox.getStyleClass().add("small-search-box");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("onboarding-table-view");
        int tableHeight = 240;
        tableView.setMinHeight(tableHeight);
        int height = 650;
        tableView.setMaxWidth(height);
        configTableView();

        StackPane.setMargin(searchBox, new Insets(0, 0, tableHeight - 3, height - searchBoxWidth));

        StackPane tableViewWithSearchBox = new StackPane(tableView, searchBox);
        tableViewWithSearchBox.setMaxWidth(height);

        VBox.setMargin(headLineLabel, new Insets(38, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 20, 0));
        VBox.setMargin(tableViewWithSearchBox, new Insets(0, 0, 30, 0));
        root.getChildren().addAll(headLineLabel, subtitleLabel, tableViewWithSearchBox/*, Spacer.fillVBox()*/);
    }

    @Override
    protected void onViewAttached() {
        searchField.textProperty().bindBidirectional(model.getSearchText());
    }

    @Override
    protected void onViewDetached() {
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("onboarding.market.columns.name"))
                .isFirst()
                .minWidth(100)
                .comparator(Comparator.comparing(MarketListItem::getMarketCodes))
                .setCellFactory(getNameCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("onboarding.market.columns.numOffers"))
                .minWidth(60)
                .valueSupplier(MarketListItem::getNumOffers)
                .comparator(Comparator.comparing(MarketListItem::getNumOffersAsInteger))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("onboarding.market.columns.numPeers"))
                .minWidth(60)
                .valueSupplier(MarketListItem::getNumUsers)
                .comparator(Comparator.comparing(MarketListItem::getNumUsersAsInteger))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .fixWidth(140)
                .setCellFactory(getSelectionCellFactory())
                .build());
    }

    private Callback<TableColumn<MarketListItem, MarketListItem>, TableCell<MarketListItem, MarketListItem>> getSelectionCellFactory() {
        return column -> new TableCell<>() {
            private Subscription selectionPin;
            private final BisqToggleButton toggleButton = new BisqToggleButton();

            {
                toggleButton.getStyleClass().add("onboarding-market-table-toggle-button");
            }

            @Override
            public void updateItem(final MarketListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    toggleButton.setText(Res.get("select"));
                    toggleButton.setSelected(item.getSelected().get());
                    toggleButton.setOnAction(e -> controller.onSelect(item));
                    selectionPin = EasyBind.subscribe(model.getSelectedMarketListItem(), selectedItem -> {
                        if (selectedItem != null) {
                            boolean isSelected = selectedItem.equals(item);
                            toggleButton.setSelected(isSelected);
                        }
                    });
                    setGraphic(toggleButton);
                } else {
                    toggleButton.setSelected(false);
                    toggleButton.setOnAction(null);
                    selectionPin.unsubscribe();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<MarketListItem, MarketListItem>, TableCell<MarketListItem, MarketListItem>> getNameCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setGraphicTextGap(8);
                label.getStyleClass().add("bisq-text-8");
            }

            @Override
            public void updateItem(final MarketListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setGraphic(item.getIcon());
                    label.setText(item.getMarketCodes());
                    label.setTooltip(new Tooltip(item.getMarketName()));
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
        private final int numOffersAsInteger;
        private final int numUsersAsInteger;
        private final String numOffers;
        private final String numUsers;
        @EqualsAndHashCode.Exclude
        private final StackPane icon;
        @EqualsAndHashCode.Exclude
        private final BooleanProperty selected = new SimpleBooleanProperty();

        MarketListItem(Market market, int numOffersAsInteger, int numUsersAsInteger) {
            this.market = market;
            this.numOffers = String.valueOf(numOffersAsInteger);
            this.numOffersAsInteger = numOffersAsInteger;
            this.numUsers = String.valueOf(numUsersAsInteger);
            icon = MarketImageComposition.imageBoxForMarket(
                    market.getBaseCurrencyCode().toLowerCase(),
                    market.getQuoteCurrencyCode().toLowerCase());
            icon.getStyleClass().add("onboarding-table-view");
            this.numUsersAsInteger = numUsersAsInteger;
        }

        public String getMarketCodes() {
            return market.getMarketCodes();
        }

        public String getMarketName() {
            return market.getMarketName();
        }

        @Override
        public String toString() {
            return market.toString();
        }
    }
}
