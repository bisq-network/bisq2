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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.desktop.common.Icons;
import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ComboBoxWithSearch;
import bisq.desktop.main.content.chat.BaseChatView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BisqEasyOfferbookView extends BaseChatView {
    private static final double CHAT_BOX_MAX_WIDTH = 1440;
    // private static double filterPaneHeight;

    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private final BisqEasyOfferbookController bisqEasyOfferbookController;
    //private Switch offersOnlySwitch;
    //private Button closeFilterButton, filterButton;

    private Label marketSelectorIcon;

    //private Pane filterPane;
   /* private Subscription showFilterOverlayPin;
    private Subscription filterPaneHeightPin;
*/
    public BisqEasyOfferbookView(BisqEasyOfferbookModel model,
                                 BisqEasyOfferbookController controller,
                                 VBox chatMessagesComponent,
                                 Pane channelSidebar) {
        super(model,
                controller,
                chatMessagesComponent,
                channelSidebar);

        bisqEasyOfferbookController = controller;
        bisqEasyOfferbookModel = model;
    }

    @Override
    protected void configTitleHBox() {
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        titleHBox.getStyleClass().add("bisq-easy-container-header");
        titleHBox.setMinHeight(HEADER_HEIGHT);
        titleHBox.setMaxHeight(HEADER_HEIGHT);

        channelTitle.getStyleClass().add("chat-header-title");
        channelTitle.setCursor(Cursor.HAND);
        channelTitle.setMinWidth(81);

        marketSelectorIcon = Icons.getIcon(AwesomeIcon.CHEVRON_DOWN, "12");
        marketSelectorIcon.getStyleClass().add("market-selector-icon");
        marketSelectorIcon.setCursor(Cursor.HAND);
        marketSelectorIcon.setPadding(new Insets(0, 15, 0, 7));
        marketSelectorIcon.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.selectMarket"), true));

        channelDescription.getStyleClass().addAll("chat-header-description");

        HBox headerTitle = new HBox(channelTitle, marketSelectorIcon, channelDescription);
        headerTitle.setAlignment(Pos.BASELINE_LEFT);
        headerTitle.setPadding(new Insets(7, 0, 0, 0));
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        searchBox.setMaxWidth(200);
        searchBox.setMaxHeight(searchBox.getMinHeight());
        searchBox.setDefaultStyle("bisq-easy-offerbook-search-box");
        searchBox.setActiveStyle("bisq-easy-offerbook-search-box-active");
        searchBox.setActiveIconId("search-green");

        double scale = 1.15;
        helpButton = BisqIconButton.createIconButton("icon-help");
        helpButton.setScaleX(scale);
        helpButton.setScaleY(scale);
        infoButton = BisqIconButton.createIconButton("icon-info");
        infoButton.setScaleX(scale);
        infoButton.setScaleY(scale);

        HBox.setMargin(channelTitle, new Insets(0, -10, 0, 4));
        HBox.setMargin(helpButton, new Insets(-2, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(-2, 0, 0, 0));
        titleHBox.getChildren().addAll(headerTitle, searchBox, helpButton, infoButton);
    }

    @Override
    protected void configCenterVBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);
        centerVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);

       /* filterButton = new Button(Res.get("bisqEasy.topPane.filter"));
        ImageView filterIcon = ImageUtil.getImageViewById("filter");
        filterIcon.setOpacity(0.3);
        filterButton.setAlignment(Pos.CENTER_LEFT);
        filterButton.setTextAlignment(TextAlignment.LEFT);
        filterButton.setPadding(new Insets(0, -110, 0, 0));
        filterButton.setGraphic(filterIcon);
        filterButton.setGraphicTextGap(10);
        filterButton.getStyleClass().add("grey-transparent-outlined-button");
        filterButton.setStyle("-fx-padding: 5 12 5 12;");*/

        // offersOnlySwitch = new Switch(Res.get("bisqEasy.topPane.filter.offersOnly"));

       /* Label filterLabel = new Label(Res.get("bisqEasy.topPane.filter"));
        filterLabel.getStyleClass().add("bisq-easy-chat-filter-headline");
        closeFilterButton = BisqIconButton.createIconButton("close");*/

       /* HBox.setMargin(closeFilterButton, new Insets(0, 0, 0, 0));
        HBox headlineAndCloseButton = new HBox(filterLabel, Spacer.fillHBox(), closeFilterButton);
        headlineAndCloseButton.setAlignment(Pos.CENTER);*/

      /*  filterPane = new VBox(20, headlineAndCloseButton, searchBox, offersOnlySwitch);
        filterPane.getStyleClass().add("bisq-easy-chat-filter-panel-bg");
        filterPane.setPadding(new Insets(10));*/

        chatMessagesComponent.setMinWidth(700);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), chatMessagesComponent);
        centerVBox.getStyleClass().add("bisq-easy-container");
    }

    @Override
    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-easy-chat-sidebar-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    @Override
    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        Layout.pinToAnchorPane(containerHBox, 0, 0, 0, 0);

        AnchorPane wrapper = new AnchorPane();
        wrapper.setPadding(new Insets(0, 40, 0, 40));
        wrapper.getChildren().add(containerHBox);

        root.setContent(wrapper);

        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);
        containerHBox.setAlignment(Pos.CENTER);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        // offersOnlySwitch.selectedProperty().bindBidirectional(bisqEasyOfferbookModel.getOfferOnly());

      /*  if (filterPaneHeight == 0) {
            filterPaneHeightPin = EasyBind.subscribe(filterPane.heightProperty(), h -> {
                if (h.doubleValue() > 0) {
                    filterPaneHeight = h.doubleValue();
                    double target = bisqEasyOfferbookModel.getShowFilterOverlay().get() ? filterPaneHeight : 0;
                    filterPane.setMinHeight(target);
                    filterPane.setMaxHeight(target);
                    filterPaneHeightPin.unsubscribe();
                }
            });
        } else {
            double target = bisqEasyOfferbookModel.getShowFilterOverlay().get() ? filterPaneHeight : 0;
            filterPane.setMinHeight(target);
            filterPane.setMaxHeight(target);
        }*/
       /* showFilterOverlayPin = EasyBind.subscribe(bisqEasyOfferbookModel.getShowFilterOverlay(),
                showFilterOverlay -> {
                    if (filterPaneHeight > 0) {
                        if (showFilterOverlay) {
                            filterButton.setText(Res.get("bisqEasy.topPane.closeFilter"));
                            root.getScene().setOnKeyReleased(keyEvent -> KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, bisqEasyOfferbookController::onCloseFilter));
                        } else {
                            filterButton.setText(Res.get("bisqEasy.topPane.filter"));
                            root.getScene().setOnKeyReleased(null);
                        }
                        double target = showFilterOverlay ? filterPaneHeight : 0;
                        if (filterPane.getMaxHeight() != target) {
                            double start = showFilterOverlay ? 0 : filterPaneHeight;
                            Transitions.animateMaxHeight(filterPane, start, target, Transitions.DEFAULT_DURATION / 4d, () -> {
                            });
                        }
                    }
                });*/

        //  filterButton.setOnAction(e -> bisqEasyOfferbookController.onToggleFilter());
        //  closeFilterButton.setOnAction(e -> bisqEasyOfferbookController.onCloseFilter());
        marketSelectorIcon.setOnMouseClicked(e -> {
            onOpenMarketSelector();
            e.consume();
        });
        channelTitle.setOnMouseClicked(e -> {
            // Only handle click on text. On click of channel icon we show channel info
            if (e.getTarget() instanceof Text) {
                onOpenMarketSelector();
                e.consume();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        // offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyOfferbookModel.getOfferOnly());

        //  showFilterOverlayPin.unsubscribe();

        //  filterButton.setOnAction(null);
        //  closeFilterButton.setOnAction(null);
        marketSelectorIcon.setOnMouseClicked(null);
        channelTitle.setOnMouseClicked(null);
    }

    private void onOpenMarketSelector() {
        new ComboBoxWithSearch<>(marketSelectorIcon,
                bisqEasyOfferbookModel.getSortedMarketChannelItems(),
                c -> getMarketListCell(),
                bisqEasyOfferbookController::onSwitchMarketChannel,
                Res.get("bisqEasy.offerbook.selectMarket").toUpperCase(),
                Res.get("action.search"),
                350, 5, 23, 31.5)
                .show();
    }

    private ListCell<MarketChannelItem> getMarketListCell() {
        return new ListCell<>() {
            final Label market = new Label();
            final Label numOffers = new Label();
            final HBox hBox = new HBox(10, market, Spacer.fillHBox(), numOffers);
            final Tooltip tooltip = new BisqTooltip();

            {
                setCursor(Cursor.HAND);
                setPrefHeight(40);
                setPadding(new Insets(0, 0, -20, 0));

                market.getStyleClass().add("market-selection");

                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.setPadding(new Insets(0, 10, 0, -5));
                Tooltip.install(hBox, tooltip);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    market.setText(item.getMarketString());
                    int numMessages = bisqEasyOfferbookController.getNumMessages(item.getMarket());
                    numOffers.setText(numMessages > 0 ?
                            numMessages > 1 ?
                                    Res.get("bisqEasy.offerbook.marketListCell.numOffers.many", numMessages) :
                                    Res.get("bisqEasy.offerbook.marketListCell.numOffers.one", numMessages) :
                            "");
                    String quoteCurrencyName = item.getMarket().getQuoteCurrencyName();
                    tooltip.setText(numMessages > 0 ?
                            numMessages > 1 ?
                                    Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.many",
                                            numMessages, quoteCurrencyName) :
                                    Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.one",
                                            numMessages, quoteCurrencyName) :
                            Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.none",
                                    quoteCurrencyName));
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

}
