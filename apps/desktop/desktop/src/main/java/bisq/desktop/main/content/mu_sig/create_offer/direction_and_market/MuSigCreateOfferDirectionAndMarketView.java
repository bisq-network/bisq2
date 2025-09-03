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
import javafx.scene.control.ContentDisplay;
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
    private final Button buyButton, sellButton;
    private final BisqTableView<MarketListItem> marketsTableView;
    private final BisqTableView<BaseCryptoAssetListItem> baseCryptoAssetsTableView;
    private final SearchBox searchBox;
    private final Label currencyLabel;
    private final BisqPopup marketSelectionPopup;
    private final HBox currencyLabelBox;
    private Subscription directionSubscription, marketPin, marketSelectionPin;

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
        double tableHeight = 307;
        double tableWidth = 500;
        marketsTableView.setPrefSize(tableWidth, tableHeight);
        marketsTableView.setFixedCellSize(50);
        configTableView();

        StackPane.setMargin(searchBox, new Insets(3, 0, 0, 15));
        StackPane tableViewWithSearchBox = new StackPane(marketsTableView, searchBox);
        tableViewWithSearchBox.setAlignment(Pos.TOP_LEFT);
        tableViewWithSearchBox.setPrefSize(tableWidth, tableHeight);
        tableViewWithSearchBox.setMaxWidth(tableWidth);
        tableViewWithSearchBox.setMaxHeight(tableHeight);

        // Base crypto assets table view
        baseCryptoAssetsTableView = new BisqTableView<>(model.getSortedBaseCryptoAssetListItems());

        currencyLabel = new Label();
        currencyLabel.setGraphic(ImageUtil.getImageViewById("chevron-drop-menu-green"));
        currencyLabel.setContentDisplay(ContentDisplay.RIGHT);
        currencyLabel.getStyleClass().add("bisq-easy-trade-wizard-quote-currency");
        currencyLabelBox = new HBox(currencyLabel);
        currencyLabelBox.getStyleClass().add("currency-label-box");

        marketSelectionPopup = new BisqPopup();
        marketSelectionPopup.setContentNode(tableViewWithSearchBox);
        marketSelectionPopup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);

        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.directionAndMarket.headline"));
        headlineLabel.setPadding(new Insets(0, 5, 0, 0));
        Label questionMark = new Label("?");
        questionMark.setPadding(new Insets(0, 0, 0, 5));
        HBox headlineHBox = new HBox(headlineLabel, currencyLabelBox, questionMark);
        headlineHBox.setAlignment(Pos.CENTER);
        headlineHBox.getStyleClass().add("bisq-text-headline-2");

        buyButton = createAndGetDirectionButton(Res.get("bisqEasy.tradeWizard.directionAndMarket.buy"));
        sellButton = createAndGetDirectionButton(Res.get("bisqEasy.tradeWizard.directionAndMarket.sell"));
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
        marketsTableView.initialize();
        marketsTableView.getSelectionModel().select(model.getSelectedMarketListItem().get());
        // We use setOnMouseClicked handler not a listener on
        // tableView.getSelectionModel().getSelectedItem() to get triggered the handler only at user action and
        // not when we set the selected item by code.
        marketsTableView.setOnMouseClicked(e ->
                controller.onMarketListItemClicked(marketsTableView.getSelectionModel().getSelectedItem()));
        currencyLabel.setOnMouseClicked(e -> {
            if (!marketSelectionPopup.isShowing()) {
                Bounds rootBounds = root.localToScreen(root.getBoundsInLocal());
                Bounds labelBounds = currencyLabel.localToScreen(currencyLabel.getBoundsInLocal());
                marketSelectionPopup.show(currencyLabel, rootBounds.getMaxX() - 168, labelBounds.getMaxY() + 15);
            } else {
                marketSelectionPopup.hide();
            }
        });

        baseCryptoAssetsTableView.initialize();

        searchBox.textProperty().bindBidirectional(model.getSearchText());

        buyButton.disableProperty().bind(model.getBuyButtonDisabled());
        buyButton.setOnAction(evt -> controller.onSelectDirection(Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelectDirection(Direction.SELL));

        directionSubscription = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                buyButton.setDefaultButton(direction == Direction.BUY);
                sellButton.setDefaultButton(direction == Direction.SELL);
            }
        });

        marketPin = EasyBind.subscribe(model.getSelectedMarket(), selectedMarket -> {
            if (selectedMarket != null) {
                currencyLabel.setText(selectedMarket.getQuoteCurrencyDisplayName());
            }
        });

        marketSelectionPin = EasyBind.subscribe(marketSelectionPopup.showingProperty(), isShowing -> {
            String activePopupStyleClass = "active-popup";
            currencyLabelBox.getStyleClass().remove(activePopupStyleClass);
            if (isShowing) {
                currencyLabelBox.getStyleClass().add(activePopupStyleClass);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        marketsTableView.dispose();
        baseCryptoAssetsTableView.dispose();
        searchBox.textProperty().unbindBidirectional(model.getSearchText());
        marketsTableView.setOnMouseClicked(null);
        currencyLabel.setOnMouseClicked(null);

        buyButton.disableProperty().unbind();

        buyButton.setOnAction(null);
        sellButton.setOnAction(null);

        directionSubscription.unsubscribe();
        marketPin.unsubscribe();
        marketSelectionPin.unsubscribe();
    }

    private Button createAndGetDirectionButton(String title) {
        Button button = new Button(title);
        button.getStyleClass().add("card-button");
        button.setAlignment(Pos.CENTER);
        int width = 235;
        button.setMinWidth(width);
        button.setMinHeight(112);
        return button;
    }

    private void configTableView() {
        marketsTableView.getColumns().add(marketsTableView.getSelectionMarkerColumn());
        marketsTableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .left()
                .comparator(Comparator.comparing(MarketListItem::getQuoteCurrencyDisplayName))
                .setCellFactory(getMarketNameCellFactory())
                .build());
        marketsTableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("bisqEasy.tradeWizard.market.columns.numOffers"))
                .minWidth(120)
                .valueSupplier(MarketListItem::getNumOffers)
                .comparator(Comparator.comparing(MarketListItem::getNumOffersAsInteger))
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
            quoteCurrencyDisplayName = new FiatCurrency(market.getQuoteCurrencyCode()).getCodeAndDisplayName();
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
            String imageId = cryptoAsset.getCode().toLowerCase() + "-menu";
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
