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

package bisq.desktop.main.content.mu_sig.create_offer.direction_and_market;

import bisq.common.asset.CryptoAsset;
import bisq.common.asset.FiatCurrency;
import bisq.common.market.Market;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class MuSigCreateOfferDirectionAndMarketView extends View<StackPane, MuSigCreateOfferDirectionAndMarketModel,
        MuSigCreateOfferDirectionAndMarketController> {
    private static final double TABLE_HEIGHT = 307;
    private static final double TABLE_WIDTH = 300;

    private final Button buyButton, sellButton;
    private final BisqTableView<MarketListItem> marketsTableView;
    private final BisqTableView<BaseCryptoAssetListItem> baseCryptoAssetsTableView;
    private final SearchBox searchBox;
    private final Label headlineLabel, tradePairIconLabel;
    private final BisqPopup marketSelectionPopup;
    private final HBox tradePairBox;
    private Subscription directionPin, marketPin, marketSelectionPin, selectedMarketListItemPin,
            selectedBaseCryptoAssetListItemPin;

    public MuSigCreateOfferDirectionAndMarketView(MuSigCreateOfferDirectionAndMarketModel model,
                                                  MuSigCreateOfferDirectionAndMarketController controller) {
        super(new StackPane(), model, controller);

        searchBox = new SearchBox();
        searchBox.setPromptText(Res.get("bisqEasy.tradeWizard.market.columns.name").toUpperCase());
        searchBox.setMinWidth(170);
        searchBox.setMaxWidth(170);
        searchBox.getStyleClass().add("bisq-easy-trade-wizard-market-search");

        // Markets table view
        marketsTableView = new BisqTableView<>(model.getSortedMarketListItems());
        marketsTableView.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
        marketsTableView.setFixedCellSize(50);
        configMarketTableView();

        StackPane.setMargin(searchBox, new Insets(3, 0, 0, 15));
        StackPane tableViewWithSearchBox = new StackPane(marketsTableView, searchBox);
        tableViewWithSearchBox.setAlignment(Pos.TOP_LEFT);
        tableViewWithSearchBox.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
        tableViewWithSearchBox.setMaxWidth(TABLE_WIDTH);
        tableViewWithSearchBox.setMaxHeight(TABLE_HEIGHT);

        // Base crypto assets table view
        baseCryptoAssetsTableView = new BisqTableView<>(model.getSortedBaseCryptoAssetListItems());
        baseCryptoAssetsTableView.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
        baseCryptoAssetsTableView.setFixedCellSize(50);
        configBaseCryptoAssetsTableView();

        tradePairIconLabel = new Label();
        Label chevronIconLabel = new Label();
        chevronIconLabel.setGraphic(ImageUtil.getImageViewById("chevron-drop-menu-white"));
        chevronIconLabel.setPadding(new Insets(5, 0, -5, 0));
        tradePairBox = new HBox(5, tradePairIconLabel, chevronIconLabel);
        tradePairBox.getStyleClass().add("trade-pair-box");

        marketSelectionPopup = new BisqPopup();
        marketSelectionPopup.setContentNode(new HBox(baseCryptoAssetsTableView, tableViewWithSearchBox));
        marketSelectionPopup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);

        headlineLabel = new Label();
        headlineLabel.setPadding(new Insets(0, 5, 0, 0));
        Label questionMark = new Label("?");
        questionMark.setPadding(new Insets(0, 0, 0, 5));
        HBox headlineHBox = new HBox(headlineLabel, tradePairBox, questionMark);
        headlineHBox.setAlignment(Pos.CENTER);
        headlineHBox.getStyleClass().add("bisq-text-headline-2");

        buyButton = createAndGetDirectionButton();
        sellButton = createAndGetDirectionButton();
        HBox directionBox = new HBox(25, buyButton, sellButton);
        directionBox.setAlignment(Pos.BASELINE_CENTER);

        VBox content = new VBox(80);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(Spacer.fillVBox(), headlineHBox, directionBox, Spacer.fillVBox());

        root.getChildren().addAll(content);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-easy-trade-wizard-direction-step");
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.textProperty().bind(model.getHeadlineText());
        buyButton.textProperty().bind(model.getBuyButtonText());
        sellButton.textProperty().bind(model.getSellButtonText());

        marketsTableView.initialize();
        marketsTableView.getSelectionModel().select(model.getSelectedMarketListItem().get());
        // We use setOnMouseClicked handler not a listener on
        // tableView.getSelectionModel().getSelectedItem() to get triggered the handler only at user action and
        // not when we set the selected item by code.
        marketsTableView.setOnMouseClicked(e ->
                controller.onMarketListItemClicked(marketsTableView.getSelectionModel().getSelectedItem()));
        tradePairBox.setOnMouseClicked(e -> {
            if (!marketSelectionPopup.isShowing()) {
                Bounds rootBounds = root.localToScreen(root.getBoundsInLocal());
                Bounds labelBounds = tradePairIconLabel.localToScreen(tradePairIconLabel.getBoundsInLocal());
                marketSelectionPopup.show(tradePairIconLabel, rootBounds.getMaxX() - 118, labelBounds.getMaxY() + 15);
            } else {
                marketSelectionPopup.hide();
            }
        });

        baseCryptoAssetsTableView.initialize();
        baseCryptoAssetsTableView.setOnMouseClicked(e ->
                controller.onBaseCryptoAssetListItemClicked(baseCryptoAssetsTableView.getSelectionModel().getSelectedItem()));

        searchBox.textProperty().bindBidirectional(model.getSearchText());

        buyButton.disableProperty().bind(model.getBuyButtonDisabled());
        buyButton.setOnAction(evt -> controller.onSelectDirection(Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelectDirection(Direction.SELL));

        selectedMarketListItemPin = EasyBind.subscribe(model.getSelectedMarketListItem(),
                selected -> marketsTableView.getSelectionModel().select(selected));

        selectedBaseCryptoAssetListItemPin = EasyBind.subscribe(model.getSelectedBaseCryptoAssetListItem(),
                selected -> baseCryptoAssetsTableView.getSelectionModel().select(selected));

        directionPin = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                buyButton.setDefaultButton(direction == Direction.BUY);
                sellButton.setDefaultButton(direction == Direction.SELL);
            }
        });

        marketPin = EasyBind.subscribe(model.getSelectedMarket(), selectedMarket -> {
            if (selectedMarket != null) {
                StackPane tradePairImage = MarketImageComposition.getMarketIcons(selectedMarket);
                tradePairIconLabel.setGraphic(tradePairImage);
            }
        });

        marketSelectionPin = EasyBind.subscribe(marketSelectionPopup.showingProperty(), isShowing -> {
            String activePopupStyleClass = "active-market-selection-popup";
            tradePairBox.getStyleClass().remove(activePopupStyleClass);
            if (isShowing) {
                tradePairBox.getStyleClass().add(activePopupStyleClass);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        headlineLabel.textProperty().unbind();
        buyButton.textProperty().unbind();
        sellButton.textProperty().unbind();

        marketsTableView.dispose();
        baseCryptoAssetsTableView.dispose();
        searchBox.textProperty().unbindBidirectional(model.getSearchText());
        marketsTableView.setOnMouseClicked(null);
        tradePairBox.setOnMouseClicked(null);

        buyButton.disableProperty().unbind();

        buyButton.setOnAction(null);
        sellButton.setOnAction(null);

        selectedMarketListItemPin.unsubscribe();
        selectedBaseCryptoAssetListItemPin.unsubscribe();
        directionPin.unsubscribe();
        marketPin.unsubscribe();
        marketSelectionPin.unsubscribe();
    }

    private Button createAndGetDirectionButton() {
        Button button = new Button();
        button.getStyleClass().add("card-button");
        button.setAlignment(Pos.CENTER);
        int width = 235;
        button.setMinWidth(width);
        button.setMinHeight(112);
        return button;
    }

    private void configMarketTableView() {
        marketsTableView.getColumns().add(marketsTableView.getSelectionMarkerColumn());
        marketsTableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .left()
                .comparator(Comparator.comparing(MarketListItem::getQuoteCurrencyDisplayName))
                .setCellFactory(getMarketNameCellFactory())
                .build());
        marketsTableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("bisqEasy.tradeWizard.market.columns.numOffers"))
                .fixWidth(70)
                .valueSupplier(MarketListItem::getNumOffers)
                .comparator(Comparator.comparing(MarketListItem::getNumOffersAsInteger))
                .build());
    }

    private void configBaseCryptoAssetsTableView() {
        baseCryptoAssetsTableView.getColumns().add(baseCryptoAssetsTableView.getSelectionMarkerColumn());
        baseCryptoAssetsTableView.getColumns().add(new BisqTableColumn.Builder<BaseCryptoAssetListItem>()
                .left()
                .comparator(Comparator.comparing(BaseCryptoAssetListItem::getDisplayName))
                .setCellFactory(getBaseCryptoAssetNameCellFactory())
                .build());
        baseCryptoAssetsTableView.getColumns().add(new BisqTableColumn.Builder<BaseCryptoAssetListItem>()
                .title(Res.get("bisqEasy.tradeWizard.market.columns.numOffers"))
                .fixWidth(70)
                .valueSupplier(BaseCryptoAssetListItem::getNumOffers)
                .comparator(Comparator.comparing(BaseCryptoAssetListItem::getNumOffersAsInteger))
                .build());
    }

    private Callback<TableColumn<MarketListItem, MarketListItem>,
            TableCell<MarketListItem, MarketListItem>> getMarketNameCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new BisqTooltip();

            {
                label.setPadding(new Insets(0, 0, 0, 10));
                label.setGraphicTextGap(8);
                label.getStyleClass().add("market-name");
                tooltip.getStyleClass().add("market-name-tooltip");
            }

            @Override
            protected void updateItem(MarketListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setGraphic(item.getMarketLogo());
                    String quoteCurrencyName = item.getQuoteCurrencyDisplayName();
                    label.setText(quoteCurrencyName);
                    if (quoteCurrencyName.length() > 30) {
                        tooltip.setText(quoteCurrencyName);
                        label.setTooltip(tooltip);
                    }

                    setGraphic(label);
                } else {
                    label.setTooltip(null);
                    label.setGraphic(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<BaseCryptoAssetListItem, BaseCryptoAssetListItem>,
            TableCell<BaseCryptoAssetListItem, BaseCryptoAssetListItem>> getBaseCryptoAssetNameCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new BisqTooltip();

            {
                label.setPadding(new Insets(0, 0, 0, 10));
                label.setGraphicTextGap(8);
                label.getStyleClass().add("market-name");
                tooltip.getStyleClass().add("market-name-tooltip");
            }

            @Override
            protected void updateItem(BaseCryptoAssetListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setGraphic(item.getCryptoAssetLogo());
                    String quoteCurrencyName = item.getDisplayName();
                    label.setText(quoteCurrencyName);
                    if (quoteCurrencyName.length() > 30) {
                        tooltip.setText(quoteCurrencyName);
                        label.setTooltip(tooltip);
                    }

                    setGraphic(label);
                } else {
                    label.setTooltip(null);
                    label.setGraphic(null);
                    setGraphic(null);
                }
            }
        };
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    static class MarketListItem {
        @EqualsAndHashCode.Include
        private final Market market;
        @EqualsAndHashCode.Include
        private final long numOffersAsInteger;
        private final String quoteCurrencyDisplayName;
        private final String numOffers;
        private final Node marketLogo;

        MarketListItem(Market market, long numOffersAsInteger) {
            this.market = market;
            this.numOffersAsInteger = numOffersAsInteger;
            numOffers = String.valueOf(numOffersAsInteger);
            quoteCurrencyDisplayName = market.isCrypto()
                    ? new CryptoAsset(market.getQuoteCurrencyCode()).getCodeAndDisplayName()
                    : new FiatCurrency(market.getQuoteCurrencyCode()).getCodeAndDisplayName();
            marketLogo = MarketImageComposition.createMarketLogo(market.getQuoteCurrencyCode());
            marketLogo.setCache(true);
            marketLogo.setCacheHint(CacheHint.SPEED);
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.1);
            marketLogo.setEffect(colorAdjust);
        }

        @Override
        public String toString() {
            return market.toString();
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    static class BaseCryptoAssetListItem {
        @EqualsAndHashCode.Include
        private final CryptoAsset cryptoAsset;
        @EqualsAndHashCode.Include
        private final long numOffersAsInteger;
        private final String displayName;
        private final String numOffers;
        private final Label cryptoAssetLogo;

        BaseCryptoAssetListItem(CryptoAsset cryptoAsset, long numOffersAsInteger) {
            this.cryptoAsset = cryptoAsset;
            this.numOffersAsInteger = numOffersAsInteger;
            numOffers = String.valueOf(numOffersAsInteger);
            displayName = cryptoAsset.getCodeAndDisplayName();
            cryptoAssetLogo = new Label();
            String imageId = String.format("market-%s", cryptoAsset.getCode().toLowerCase());
            cryptoAssetLogo.setGraphic(ImageUtil.getImageViewById(imageId));
            cryptoAssetLogo.setCache(true);
            cryptoAssetLogo.setCacheHint(CacheHint.SPEED);
            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.1);
            cryptoAssetLogo.setEffect(colorAdjust);
        }

        @Override
        public String toString() {
            return cryptoAsset.toString();
        }
    }
}
