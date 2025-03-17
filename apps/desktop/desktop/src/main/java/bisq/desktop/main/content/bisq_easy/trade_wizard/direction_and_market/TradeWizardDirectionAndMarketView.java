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

package bisq.desktop.main.content.bisq_easy.trade_wizard.direction_and_market;

import bisq.common.currency.FiatCurrency;
import bisq.common.currency.Market;
import bisq.desktop.common.Icons;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import bisq.offer.Direction;
import de.jensd.fx.fontawesome.AwesomeIcon;
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
import javafx.scene.text.TextAlignment;
import javafx.stage.PopupWindow;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class TradeWizardDirectionAndMarketView extends View<StackPane, TradeWizardDirectionAndMarketModel,
        TradeWizardDirectionAndMarketController> {
    private final Button buyButton, sellButton;
    private final VBox reputationInfo, content;
    private final BisqTableView<TradeWizardDirectionAndMarketView.ListItem> tableView;
    private final SearchBox searchBox;
    private final Label currencyLabel;
    private final BisqPopup marketSelectionPopup;
    private final HBox currencyLabelBox;
    private Subscription directionSubscription, showReputationInfoPin, marketPin, marketSelectionPin;
    private Button backToBuyButton, gainReputationButton;

    public TradeWizardDirectionAndMarketView(TradeWizardDirectionAndMarketModel model,
                                             TradeWizardDirectionAndMarketController controller) {
        super(new StackPane(), model, controller);

        searchBox = new SearchBox();
        searchBox.setPromptText(Res.get("bisqEasy.tradeWizard.market.columns.name").toUpperCase());
        searchBox.setMinWidth(170);
        searchBox.setMaxWidth(170);
        searchBox.getStyleClass().add("bisq-easy-trade-wizard-market-search");

        tableView = new BisqTableView<>(model.getSortedList());
        double tableHeight = 307;
        double tableWidth = 500;
        tableView.setPrefSize(tableWidth, tableHeight);
        tableView.setFixedCellSize(50);
        configTableView();

        StackPane.setMargin(searchBox, new Insets(3, 0, 0, 15));
        StackPane tableViewWithSearchBox = new StackPane(tableView, searchBox);
        tableViewWithSearchBox.setAlignment(Pos.TOP_LEFT);
        tableViewWithSearchBox.setPrefSize(tableWidth, tableHeight);
        tableViewWithSearchBox.setMaxWidth(tableWidth);
        tableViewWithSearchBox.setMaxHeight(tableHeight);

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

        content = new VBox(80);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(Spacer.fillVBox(), headlineHBox, directionBox, Spacer.fillVBox());

        reputationInfo = new VBox(20);
        setupReputationInfo();

        StackPane.setMargin(reputationInfo, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, reputationInfo);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-easy-trade-wizard-direction-step");
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
        tableView.getSelectionModel().select(model.getSelectedMarketListItem().get());
        // We use setOnMouseClicked handler not a listener on
        // tableView.getSelectionModel().getSelectedItem() to get triggered the handler only at user action and
        // not when we set the selected item by code.
        tableView.setOnMouseClicked(e -> controller.onMarketListItemClicked(tableView.getSelectionModel().getSelectedItem()));
        currencyLabel.setOnMouseClicked(e -> {
            if (!marketSelectionPopup.isShowing()) {
                Bounds rootBounds = root.localToScreen(root.getBoundsInLocal());
                Bounds labelBounds = currencyLabel.localToScreen(currencyLabel.getBoundsInLocal());
                marketSelectionPopup.show(currencyLabel, rootBounds.getMaxX() - 168, labelBounds.getMaxY() + 15);
            } else {
                marketSelectionPopup.hide();
            }
        });

        searchBox.textProperty().bindBidirectional(model.getSearchText());

        buyButton.disableProperty().bind(model.getBuyButtonDisabled());
        buyButton.setOnAction(evt -> controller.onSelectDirection(Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelectDirection(Direction.SELL));
        gainReputationButton.setOnAction(evt -> controller.onBuildReputation());
        backToBuyButton.setOnAction(evt -> controller.onCloseReputationInfo());

        directionSubscription = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                buyButton.setDefaultButton(direction == Direction.BUY);
                sellButton.setDefaultButton(direction == Direction.SELL);
            }
        });

        showReputationInfoPin = EasyBind.subscribe(model.getShowReputationInfo(),
                showReputationInfo -> {
                    if (showReputationInfo) {
                        reputationInfo.setVisible(true);
                        reputationInfo.setOpacity(1);
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(reputationInfo, 450);
                    } else {
                        Transitions.removeEffect(content);
                        if (reputationInfo.isVisible()) {
                            Transitions.fadeOut(reputationInfo, Transitions.DEFAULT_DURATION / 2,
                                    () -> reputationInfo.setVisible(false));
                        }
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
        tableView.dispose();
        searchBox.textProperty().unbindBidirectional(model.getSearchText());
        tableView.setOnMouseClicked(null);
        currencyLabel.setOnMouseClicked(null);

        if (model.getShowReputationInfo().get()) {
            Transitions.removeEffect(content);
        }
        buyButton.disableProperty().unbind();

        buyButton.setOnAction(null);
        sellButton.setOnAction(null);
        gainReputationButton.setOnAction(null);
        backToBuyButton.setOnAction(null);

        directionSubscription.unsubscribe();
        showReputationInfoPin.unsubscribe();
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

    private void setupReputationInfo() {
        double width = 700;
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("trade-wizard-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(width);

        // We don't use setManaged as the transition would not work as expected if set to false
        reputationInfo.setVisible(false);
        reputationInfo.setAlignment(Pos.TOP_CENTER);
        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.directionAndMarket.feedback.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        headlineLabel.setTextAlignment(TextAlignment.CENTER);
        headlineLabel.setAlignment(Pos.CENTER);
        headlineLabel.setMaxWidth(width - 60);

        Label warningIcon = new Label();
        Icons.getIconForLabel(AwesomeIcon.WARNING_SIGN, warningIcon, "1.7em");
        warningIcon.getStyleClass().add("text-fill-light-dimmed");

        HBox headlineBox = new HBox(15, warningIcon, headlineLabel);
        headlineBox.setAlignment(Pos.CENTER);

        Label subtitleLabel1 = new Label(Res.get("bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle1"));
        subtitleLabel1.setMaxWidth(width - 60);
        subtitleLabel1.getStyleClass().addAll("bisq-text-21", "wrap-text");

        Label subtitleLabel2 = new Label(Res.get("bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle2"));
        subtitleLabel2.setMaxWidth(width - 60);
        subtitleLabel2.getStyleClass().addAll("bisq-text-21", "wrap-text");

        Label subtitleLabel3 = new Label(Res.get("bisqEasy.tradeWizard.directionAndMarket.feedback.subTitle3"));
        subtitleLabel3.setMaxWidth(width - 60);
        subtitleLabel3.getStyleClass().addAll("bisq-text-21", "wrap-text");

        backToBuyButton = new Button(Res.get("bisqEasy.tradeWizard.directionAndMarket.feedback.backToBuy"));
        gainReputationButton = new Button(Res.get("bisqEasy.tradeWizard.directionAndMarket.feedback.gainReputation"));
        gainReputationButton.setDefaultButton(true);

        HBox buttons = new HBox(7, backToBuyButton, gainReputationButton);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(headlineBox, new Insets(20, 0, 20, 0));
        VBox.setMargin(buttons, new Insets(30, 0, 0, 0));
        contentBox.getChildren().addAll(headlineBox,
                subtitleLabel1,
                subtitleLabel2,
                subtitleLabel3,
                buttons);
        reputationInfo.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeWizardDirectionAndMarketView.ListItem>()
                .left()
                .comparator(Comparator.comparing(TradeWizardDirectionAndMarketView.ListItem::getQuoteCurrencyDisplayName))
                .setCellFactory(getNameCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeWizardDirectionAndMarketView.ListItem>()
                .title(Res.get("bisqEasy.tradeWizard.market.columns.numOffers"))
                .minWidth(120)
                .valueSupplier(TradeWizardDirectionAndMarketView.ListItem::getNumOffers)
                .comparator(Comparator.comparing(TradeWizardDirectionAndMarketView.ListItem::getNumOffersAsInteger))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<TradeWizardDirectionAndMarketView.ListItem>()
                .title(Res.get("bisqEasy.tradeWizard.market.columns.numPeers"))
                .minWidth(120)
                .valueSupplier(TradeWizardDirectionAndMarketView.ListItem::getNumUsers)
                .comparator(Comparator.comparing(TradeWizardDirectionAndMarketView.ListItem::getNumUsersAsInteger))
                .build());
    }

    private Callback<TableColumn<TradeWizardDirectionAndMarketView.ListItem, TradeWizardDirectionAndMarketView.ListItem>,
            TableCell<TradeWizardDirectionAndMarketView.ListItem, TradeWizardDirectionAndMarketView.ListItem>> getNameCellFactory() {
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
            protected void updateItem(TradeWizardDirectionAndMarketView.ListItem item, boolean empty) {
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
    static class ListItem {
        @EqualsAndHashCode.Include
        private final Market market;
        @EqualsAndHashCode.Include
        private final int numOffersAsInteger;
        @EqualsAndHashCode.Include
        private final int numUsersAsInteger;

        private final String quoteCurrencyDisplayName;
        private final String numOffers;
        private final String numUsers;

        // TODO: move to cell
        private final Node marketLogo;

        ListItem(Market market, int numOffersAsInteger, int numUsersAsInteger) {
            this.market = market;
            this.numOffersAsInteger = numOffersAsInteger;
            this.numUsersAsInteger = numUsersAsInteger;

            this.numOffers = String.valueOf(numOffersAsInteger);
            quoteCurrencyDisplayName = new FiatCurrency(market.getQuoteCurrencyCode()).getCodeAndDisplayName();
            this.numUsers = String.valueOf(numUsersAsInteger);
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
}
