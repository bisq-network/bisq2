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

import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ComboBoxWithSearch;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class BisqEasyOfferbookView extends ChatView<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    // private static double filterPaneHeight;

    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private final BisqEasyOfferbookController bisqEasyOfferbookController;
    private Label chatDomainTitle;
    private BisqTableView<MarketChannelItem> tableView;
    private VBox marketSelectionList;
    private HBox marketSelectionListHeader;
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
        super(model, controller, chatMessagesComponent, channelSidebar);

        bisqEasyOfferbookController = controller;
        bisqEasyOfferbookModel = model;
    }

    @Override
    protected void configTitleHBox() {
        super.configTitleHBox();

        chatDomainTitle = new Label(Res.get("bisqEasy.offerbook"));
        chatDomainTitle.getStyleClass().add("chat-header-title");

        HBox headerTitle = new HBox(10, chatDomainTitle, channelDescription);
        headerTitle.setAlignment(Pos.BASELINE_LEFT);
        headerTitle.setPadding(new Insets(7, 0, 0, 0));
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        titleHBox.getChildren().setAll(headerTitle, searchBox, headerDropdownMenu);
    }

    @Override
    protected void configCenterVBox() {
        addMarketSelectionList();
        addChatBox();
    }

    @Override
    protected void configContainerHBox() {
        super.configContainerHBox();

        containerHBox.getChildren().setAll(marketSelectionList, centerVBox, sideBar);
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

//        marketSelectorIcon.setOnMouseClicked(e -> {
//            onOpenMarketSelector();
//            e.consume();
//        });
        chatDomainTitle.setOnMouseClicked(e -> { // TODO: Remove
            onOpenMarketSelector();
            e.consume();
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        // offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyOfferbookModel.getOfferOnly());

        //  showFilterOverlayPin.unsubscribe();

        //  filterButton.setOnAction(null);
        //  closeFilterButton.setOnAction(null);
//        marketSelectorIcon.setOnMouseClicked(null);
        chatDomainTitle.setOnMouseClicked(null);
    }

    private BisqEasyOfferbookModel getModel() {
        return (BisqEasyOfferbookModel) model;
    }

    private void addMarketSelectionList() {
        marketSelectionListHeader = new HBox(channelTitle);
        marketSelectionListHeader.setMinHeight(HEADER_HEIGHT);
        marketSelectionListHeader.setMaxHeight(HEADER_HEIGHT);
        marketSelectionListHeader.setAlignment(Pos.CENTER_LEFT);
        marketSelectionListHeader.setPadding(new Insets(12.5, 25, 12.5, 25));
        marketSelectionListHeader.getStyleClass().add("chat-header-title");

        tableView = new BisqTableView<>(getModel().getSortedMarketChannelItems());
        tableView.allowVerticalScrollbar();
        configTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        marketSelectionList = new VBox(marketSelectionListHeader, Layout.hLine(), tableView);
        marketSelectionList.setPrefWidth(210);
        marketSelectionList.setMinWidth(210);
        marketSelectionList.setFillWidth(true);
        marketSelectionList.getStyleClass().add("chat-container");
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<MarketChannelItem>()
                .minWidth(100)
                .left()
                //.comparator(Comparator.comparing(PrivateChatsView.ListItem::getPeersUserName))
                //.setCellFactory(new PropertyValueFactory<MarketChannelItem, String>("marketChannelItem"))
                .build());
    }

    private void addChatBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

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
        centerVBox.setAlignment(Pos.CENTER);
    }

    private void onOpenMarketSelector() {
        new ComboBoxWithSearch<>(chatDomainTitle,
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
