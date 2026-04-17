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

package bisq.desktop.main.content.mu_sig.offer.create_offer.direction_and_market;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow;
import javafx.util.Callback;
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
    private final BisqTableView<MarketTypeListItem> marketTypeTableView;
    private final SearchBox paymentCurrencySearchBox;
    private final Label headlineLabel, tradePairIconLabel;
    private final BisqPopup marketSelectionPopup;
    private final HBox tradePairBox;
    private Subscription displayDirectionPin, marketSelectionPin, selectedMarketListItemPin,
            selectedMarketTypePin;

    public MuSigCreateOfferDirectionAndMarketView(MuSigCreateOfferDirectionAndMarketModel model,
                                                  MuSigCreateOfferDirectionAndMarketController controller) {
        super(new StackPane(), model, controller);

        marketsTableView = new BisqTableView<>(model.getSortedMarketListItems());
        marketsTableView.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
        marketsTableView.setFixedCellSize(50);
        configMarketTableView();
        paymentCurrencySearchBox = createAndGetSearchBox("muSig.offer.create.directionAndMarket.marketsPopup.search.payment");
        StackPane marketsTableViewWithSearchBox = createAndGetTableViewWithSearchBox(marketsTableView, paymentCurrencySearchBox);
        marketsTableViewWithSearchBox.getStyleClass().add("markets-table-view-box");

        marketTypeTableView = new BisqTableView<>(model.getSortedMarketTypeListItems());
        marketTypeTableView.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
        marketTypeTableView.setFixedCellSize(50);
        configMarketTypeTableView();

        marketSelectionPopup = new BisqPopup();
        marketSelectionPopup.setContentNode(new HBox(marketTypeTableView, marketsTableViewWithSearchBox));
        marketSelectionPopup.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_RIGHT);

        tradePairIconLabel = new Label();
        Label chevronIconLabel = new Label();
        chevronIconLabel.setGraphic(ImageUtil.getImageViewById("chevron-drop-menu-white"));
        chevronIconLabel.setPadding(new Insets(5, 0, -5, 0));
        tradePairBox = new HBox(5, tradePairIconLabel, chevronIconLabel);
        tradePairBox.getStyleClass().add("trade-pair-box");

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

        marketTypeTableView.initialize();
        marketTypeTableView.setOnMouseClicked(e ->
                controller.onMarketTypeListItemSelected(marketTypeTableView.getSelectionModel().getSelectedItem()));

        paymentCurrencySearchBox.textProperty().bindBidirectional(model.getPaymentCurrencySearchText());

        buyButton.disableProperty().bind(model.getBuyButtonDisabled());
        buyButton.setOnAction(evt -> controller.onSelectDirection(Direction.BUY));
        sellButton.setOnAction(evt -> controller.onSelectDirection(Direction.SELL));

        selectedMarketListItemPin = EasyBind.subscribe(model.getSelectedMarketListItem(),
                selected -> marketsTableView.getSelectionModel().select(selected));

        selectedMarketTypePin = EasyBind.subscribe(model.getSelectedMarketTypeListItem(),
                selected -> marketTypeTableView.getSelectionModel().select(selected));

        displayDirectionPin = EasyBind.subscribe(model.getDirection(), direction -> {
            if (direction != null) {
                buyButton.setDefaultButton(direction.isBuy());
                sellButton.setDefaultButton(direction.isSell());
            }
        });

        tradePairIconLabel.graphicProperty().bind(model.getTradePairImage());

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
        tradePairIconLabel.graphicProperty().unbind();

        marketsTableView.dispose();
        marketTypeTableView.dispose();
        paymentCurrencySearchBox.textProperty().unbindBidirectional(model.getPaymentCurrencySearchText());

        marketsTableView.setOnMouseClicked(null);
        tradePairBox.setOnMouseClicked(null);

        buyButton.disableProperty().unbind();

        buyButton.setOnAction(null);
        sellButton.setOnAction(null);

        selectedMarketListItemPin.unsubscribe();
        selectedMarketTypePin.unsubscribe();
        displayDirectionPin.unsubscribe();
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
                .comparator(Comparator.comparing(MarketListItem::getDisplayString))
                .setCellFactory(getMarketNameCellFactory())
                .build());
        marketsTableView.getColumns().add(new BisqTableColumn.Builder<MarketListItem>()
                .title(Res.get("muSig.offer.create.directionAndMarket.marketsPopup.numOffers"))
                .fixWidth(70)
                .valueSupplier(MarketListItem::getNumOffers)
                .comparator(Comparator.comparing(MarketListItem::getNumOffersAsInteger))
                .build());
    }

    private void configMarketTypeTableView() {
        marketTypeTableView.getColumns().add(marketTypeTableView.getSelectionMarkerColumn());
        marketTypeTableView.getColumns().add(new BisqTableColumn.Builder<MarketTypeListItem>()
                .title(Res.get("muSig.offer.create.directionAndMarket.marketsPopup.marketType"))
                .left()
                .comparator(Comparator.comparing(MarketTypeListItem::getDisplayName))
                .setCellFactory(getBaseCryptoAssetNameCellFactory())
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
                    String displayString = item.getDisplayString();
                    label.setText(displayString);
                    if (displayString.length() > 30) {
                        tooltip.setText(displayString);
                        label.setTooltip(tooltip);
                    } else {
                        tooltip.setText("");
                        label.setTooltip(null);
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

    private Callback<TableColumn<MarketTypeListItem, MarketTypeListItem>,
            TableCell<MarketTypeListItem, MarketTypeListItem>> getBaseCryptoAssetNameCellFactory() {
        return column -> new TableCell<>() {
            private final Label label = new Label();

            {
                label.setPadding(new Insets(0, 0, 0, 10));
                label.getStyleClass().add("market-name");
            }

            @Override
            protected void updateItem(MarketTypeListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setText(item.getDisplayName());
                    setGraphic(label);
                } else {
                    label.setGraphic(null);
                    setGraphic(null);
                }
            }
        };
    }

    private SearchBox createAndGetSearchBox(String promptText) {
        SearchBox searchBox = new SearchBox();
        searchBox.setPromptText(Res.get(promptText).toUpperCase());
        searchBox.setMinWidth(170);
        searchBox.setMaxWidth(170);
        searchBox.getStyleClass().add("bisq-easy-trade-wizard-market-search");
        return searchBox;
    }

    private StackPane createAndGetTableViewWithSearchBox(BisqTableView<?> tableView, SearchBox searchBox) {
        StackPane.setMargin(searchBox, new Insets(3, 0, 0, 15));
        StackPane stackPane = new StackPane(tableView, searchBox);
        stackPane.setAlignment(Pos.TOP_LEFT);
        stackPane.setPrefSize(TABLE_WIDTH, TABLE_HEIGHT);
        stackPane.setMaxWidth(TABLE_WIDTH);
        stackPane.setMaxHeight(TABLE_HEIGHT);
        return stackPane;
    }
}
