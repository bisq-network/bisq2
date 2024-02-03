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
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public final class BisqEasyOfferbookView extends ChatView<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    // private static double filterPaneHeight;

    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private final BisqEasyOfferbookController bisqEasyOfferbookController;
    private SearchBox marketSelectorSearchBox;
    private Label chatDomainTitle;
    private BisqTableView<MarketChannelItem> tableView;
    private BisqTableColumn<MarketChannelItem> marketsTableColumn;
    private VBox marketSelectionList;
    private Subscription tableViewSelectionPin, selectedModelItemPin;
    private Button createOfferButton;
    private DropdownMenu dropdownMenu;
    private MenuItem offers, nameAZ, nameZA;
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

        marketSelectorSearchBox.textProperty().bindBidirectional(getModel().getMarketSelectorSearchText());

        selectedModelItemPin = EasyBind.subscribe(getModel().getSelectedMarketChannelItem(), selected -> {
            tableView.getSelectionModel().select(selected);
        });

        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectMarketChannelItem(item);
            }
        });

        offers.setOnAction(e -> sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketActivity(), offers));
        nameAZ.setOnAction(e -> sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketNameAsc(), nameAZ));
        nameZA.setOnAction(e -> sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketNameDesc(), nameZA));
        createOfferButton.setOnAction(e -> getController().onCreateOffer());

        sortTableViewColumn(BisqEasyOfferbookUtil.SortByMarketActivity(), offers);
    }

    private void sortTableViewColumn(Comparator<MarketChannelItem> comparator, MenuItem menuItem) {
        dropdownMenu.setLabel(menuItem.getText());
        tableView.getSortOrder().clear();
        marketsTableColumn.setComparator(comparator);
        tableView.getSortOrder().add(marketsTableColumn);
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        // offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyOfferbookModel.getOfferOnly());

        //  showFilterOverlayPin.unsubscribe();

        //  filterButton.setOnAction(null);
        //  closeFilterButton.setOnAction(null);
//        marketSelectorIcon.setOnMouseClicked(null);

        marketSelectorSearchBox.textProperty().unbindBidirectional(getModel().getMarketSelectorSearchText());

        chatDomainTitle.setOnMouseClicked(null);

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();

        offers.setOnAction(null);
        nameAZ.setOnAction(null);
        nameZA.setOnAction(null);
        createOfferButton.setOnAction(null);
    }

    private BisqEasyOfferbookModel getModel() {
        return (BisqEasyOfferbookModel) model;
    }

    private BisqEasyOfferbookController getController() {
        return (BisqEasyOfferbookController) controller;
    }

    private void addMarketSelectionList() {
        HBox header = new HBox(channelTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12.5, 25, 12.5, 25));
        header.getStyleClass().add("chat-header-title");

        marketSelectorSearchBox = new SearchBox();
        marketSelectorSearchBox.getStyleClass().add("market-selection-search-box");

        dropdownMenu = new DropdownMenu("arrow-down", "arrow-down", false);
        dropdownMenu.getStyleClass().add("market-selection-dropdown-menu");
        offers = new MenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.offers"));
        nameAZ = new MenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.nameAZ"));
        nameZA = new MenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.nameZA"));
        dropdownMenu.addMenuItems(offers, nameAZ, nameZA);

        HBox subheader = new HBox(5, marketSelectorSearchBox, dropdownMenu);
        subheader.maxHeight(30);
        subheader.setPadding(new Insets(4, 5, 4, 5));
        subheader.getStyleClass().add("market-selection-subheader");

        tableView = new BisqTableView<>(getModel().getSortedMarketChannelItems());
        tableView.getStyleClass().add("market-selection-list");
        tableView.allowVerticalScrollbar();
        tableView.hideHorizontalScrollbar();
        configTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        createOfferButton = createAndGetCreateOfferButton();
        HBox offerButtonContainer = new HBox(createOfferButton);
        offerButtonContainer.setAlignment(Pos.CENTER);
        offerButtonContainer.setPadding(new Insets(14, 20, 14, 20));

        marketSelectionList = new VBox(header, Layout.hLine(), subheader, tableView, offerButtonContainer);
        marketSelectionList.setPrefWidth(210);
        marketSelectionList.setMinWidth(210);
        marketSelectionList.setFillWidth(true);
        marketSelectionList.getStyleClass().add("chat-container");
    }

    private Button createAndGetCreateOfferButton() {
        Button createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        createOfferButton.setMinWidth(170);

        double height = 42;
        createOfferButton.setMinHeight(height);
        createOfferButton.setMaxHeight(height);
        createOfferButton.setPrefHeight(height);
        return createOfferButton;
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        marketsTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .minWidth(100)
                .left()
                .setCellFactory(getMarketChannelItemCellFactory())
                .build();
        tableView.getColumns().add(marketsTableColumn);
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

    private Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketChannelItemCellFactory() {
        return column -> new TableCell<>() {
            private final Label market = new Label();
            private final Label numOffers = new Label();
            private final HBox hBox = new HBox(10, market, numOffers);
            private final Tooltip tooltip = new BisqTooltip();

            {
                market.setGraphicTextGap(10);
                setCursor(Cursor.HAND);
                hBox.setPadding(new Insets(10));
                //hBox.setStyle("-fx-background-color: black;");
                hBox.setAlignment(Pos.CENTER_LEFT);
                Tooltip.install(hBox, tooltip);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    market.setText(item.getMarket().getQuoteCurrencyCode());
                    market.setGraphic(item.getIcon());
                    StringExpression formattedNumOffers = Bindings.createStringBinding(() ->
                            getFormattedOfferNumber(item.getNumOffers().get()), item.getNumOffers());
                    numOffers.textProperty().bind(formattedNumOffers);
                    StringExpression formattedTooltip = Bindings.createStringBinding(() ->
                            getFormattedTooltip(item.getNumOffers().get(), item), item.getNumOffers());
                    tooltip.textProperty().bind(formattedTooltip);
                    tooltip.setStyle("-fx-text-fill: -fx-dark-text-color;");

                    setGraphic(hBox);
                } else {
                    numOffers.textProperty().unbind();
                    tooltip.textProperty().unbind();

                    setGraphic(null);
                }
            }

            private String getFormattedOfferNumber(int numMessages) {
                if (numMessages == 0) {
                    return "";
                }
                return String.format("(%s)",
                        numMessages > 1
                                ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.many", numMessages)
                                : Res.get("bisqEasy.offerbook.marketListCell.numOffers.one", numMessages)
                );
            }

            private String getFormattedTooltip(int numMessages, MarketChannelItem item) {
                String quoteCurrencyName = item.getMarket().getQuoteCurrencyName();
                if (numMessages == 0) {
                    return Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.none", quoteCurrencyName);
                }
                return numMessages > 1
                        ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.many", numMessages, quoteCurrencyName)
                        : Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.one", numMessages, quoteCurrencyName);
            }
        };
    }
}
