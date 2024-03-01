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

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.controls.DropdownTitleMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.i18n.Res;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public final class BisqEasyOfferbookView extends ChatView<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private final BisqEasyOfferbookController bisqEasyOfferbookController;
    private SearchBox marketSelectorSearchBox;
    private Label chatDomainTitle;
    private BisqTableView<MarketChannelItem> tableView;
    private BisqTableColumn<MarketChannelItem> marketLabelTableColumn;
    private VBox marketSelectionList;
    private Subscription tableViewSelectionPin, selectedModelItemPin, marketSelectorHeaderIconPin, selectedMarketFilterPin,
        selectedOffersFilterPin;
    private Button createOfferButton;
    private DropdownMenu sortAndFilterMarketsMenu, offersFilterMenu;
    private DropdownSortByMenuItem sortByMostOffers, sortByNameAZ, sortByNameZA;
    private DropdownFilterMenuItem<MarketChannelItem> filterShowAll, filterWithOffers;
    private DropdownFilterMenuItem<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>
            allOffers, myOffers, buyOffers, sellOffers;
    private CheckBox hideUserMessagesCheckbox;

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

        hideUserMessagesCheckbox.selectedProperty().bindBidirectional(bisqEasyOfferbookModel.getOfferOnly());
        marketSelectorSearchBox.textProperty().bindBidirectional(getModel().getMarketSelectorSearchText());

        selectedModelItemPin = EasyBind.subscribe(getModel().getSelectedMarketChannelItem(), selected -> {
            tableView.getSelectionModel().select(selected);
        });
        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectMarketChannelItem(item);
            }
        });
        marketSelectorHeaderIconPin = EasyBind.subscribe(model.getChannelIconNode(), this::updateMarketSelectorHeaderIcon);
        selectedMarketFilterPin = EasyBind.subscribe(getModel().getSelectedMarketsFilter(), this::updateSelectedMarketFilter);
        selectedOffersFilterPin = EasyBind.subscribe(getModel().getSelectedOffersFilter(), this::updateSelectedOffersFilter);

        sortByMostOffers.setOnAction(e -> sortTableViewColumn(sortByMostOffers));
        sortByNameAZ.setOnAction(e -> sortTableViewColumn(sortByNameAZ));
        sortByNameZA.setOnAction(e -> sortTableViewColumn(sortByNameZA));
        filterWithOffers.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.WITH_OFFERS));
        filterShowAll.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.ALL));

        allOffers.setOnAction(e -> setOffersFilter(allOffers));
        myOffers.setOnAction(e -> setOffersFilter(myOffers));
        buyOffers.setOnAction(e -> setOffersFilter(buyOffers));
        sellOffers.setOnAction(e -> setOffersFilter(sellOffers));

        createOfferButton.setOnAction(e -> getController().onCreateOffer());

        maybeSelectSorting();
    }

    private void setOffersFilter(DropdownFilterMenuItem<?> filterMenuItem) {
        getModel().getSelectedOffersFilter().set((Filters.Offers) filterMenuItem.getFilter());
    }

    private void maybeSelectSorting() {
        boolean isSortingMethodSelected = sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof DropdownSortByMenuItem)
                .anyMatch(menuItem -> ((DropdownSortByMenuItem) menuItem).isSelected());
        if (!isSortingMethodSelected) {
            sortTableViewColumn(sortByMostOffers);
        }
    }

    private void sortTableViewColumn(DropdownSortByMenuItem sortByMenuItem) {
        tableView.getSortOrder().clear();
        marketLabelTableColumn.setComparator(sortByMenuItem.getComparator());
        tableView.getSortOrder().add(marketLabelTableColumn);
        // Update dropdown state with new selection
        sortAndFilterMarketsMenu.getMenuItems().stream().filter(menuItem -> menuItem instanceof DropdownSortByMenuItem)
                .forEach(menuItem -> ((DropdownSortByMenuItem) menuItem).updateSelection(menuItem == sortByMenuItem));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        hideUserMessagesCheckbox.selectedProperty().unbindBidirectional(bisqEasyOfferbookModel.getOfferOnly());
        marketSelectorSearchBox.textProperty().unbindBidirectional(getModel().getMarketSelectorSearchText());

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        marketSelectorHeaderIconPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedOffersFilterPin.unsubscribe();

        sortByMostOffers.setOnAction(null);
        sortByNameAZ.setOnAction(null);
        sortByNameZA.setOnAction(null);
        filterWithOffers.setOnAction(null);
        filterShowAll.setOnAction(null);
        allOffers.setOnAction(null);
        myOffers.setOnAction(null);
        buyOffers.setOnAction(null);
        sellOffers.setOnAction(null);
        createOfferButton.setOnAction(null);
    }

    private BisqEasyOfferbookModel getModel() {
        return (BisqEasyOfferbookModel) model;
    }

    private BisqEasyOfferbookController getController() {
        return (BisqEasyOfferbookController) controller;
    }

    private void addMarketSelectionList() {
        channelTitle.setGraphicTextGap(8);
        HBox header = new HBox(channelTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 0, 15));
        header.getStyleClass().add("chat-header-title");

        marketSelectorSearchBox = new SearchBox();
        marketSelectorSearchBox.getStyleClass().add("market-selection-search-box");
        sortAndFilterMarketsMenu = createAndGetSortAndFilterMarketsMenu();
        HBox subheader = new HBox(marketSelectorSearchBox, Spacer.fillHBox(), sortAndFilterMarketsMenu);
        subheader.setAlignment(Pos.CENTER);
        subheader.getStyleClass().add("market-selection-subheader");

        tableView = new BisqTableView<>(getModel().getSortedMarketChannelItems());
        tableView.getStyleClass().add("market-selection-list");
        tableView.allowVerticalScrollbar();
        tableView.hideHorizontalScrollbar();
        tableView.setFixedCellSize(53);
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

    private DropdownMenu createAndGetSortAndFilterMarketsMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("sort-grey", "sort-white", true);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.tooltip"));
        dropdownMenu.getStyleClass().add("market-selection-dropdown-menu");

        // Sorting options
        DropdownTitleMenuItem sortTitle = new DropdownTitleMenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.sortTitle"));
        sortByMostOffers = new DropdownSortByMenuItem("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.mostOffers"), BisqEasyOfferbookUtil.sortByMarketActivity());
        sortByNameAZ = new DropdownSortByMenuItem("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameAZ"), BisqEasyOfferbookUtil.sortByMarketNameAsc());
        sortByNameZA = new DropdownSortByMenuItem("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameZA"), BisqEasyOfferbookUtil.sortByMarketNameDesc());

        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Filter options
        DropdownTitleMenuItem filterTitle = new DropdownTitleMenuItem(Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.filterTitle"));
        filterWithOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), Filters.Markets.WITH_OFFERS);
        filterShowAll = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.all"), Filters.Markets.ALL);

        dropdownMenu.addMenuItems(sortTitle, sortByMostOffers, sortByNameAZ, sortByNameZA, separator,
                filterTitle, filterWithOffers, filterShowAll);
        return dropdownMenu;
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
        BisqTableColumn<MarketChannelItem> marketLogoTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .fixWidth(55)
                .setCellFactory(BisqEasyOfferbookUtil.getMarketLogoCellFactory())
                .isSortable(false)
                .build();

        marketLabelTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .minWidth(100)
                .left()
                .setCellFactory(BisqEasyOfferbookUtil.getMarketLabelCellFactory())
                .build();

        tableView.getColumns().add(tableView.getSelectionMarkerColumn());
        tableView.getColumns().add(marketLogoTableColumn);
        tableView.getColumns().add(marketLabelTableColumn);
    }

    private void addChatBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        Label label = new Label(Res.get("bisqEasy.topPane.filter.hideUserMessages"));
        hideUserMessagesCheckbox = new CheckBox();
        HBox checkbox = new HBox(5, label, hideUserMessagesCheckbox);
        checkbox.getStyleClass().add("offerbook-subheader-checkbox");
        checkbox.setAlignment(Pos.CENTER);

        offersFilterMenu = createAndGetOffersFilterMenu();

        HBox subheaderContent = new HBox(30, checkbox, offersFilterMenu);
        subheaderContent.getStyleClass().add("offerbook-subheader-content");
        HBox.setHgrow(subheaderContent, Priority.ALWAYS);

        HBox subheader = new HBox(subheaderContent);
        subheader.getStyleClass().add("offerbook-subheader");
        subheader.setAlignment(Pos.CENTER);

        chatMessagesComponent.setMinWidth(700);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), subheader, chatMessagesComponent);
        centerVBox.getStyleClass().add("bisq-easy-container");
        centerVBox.setAlignment(Pos.CENTER);
    }

    private DropdownMenu createAndGetOffersFilterMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("arrow-down", "arrow-down", false);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterMarketOffers.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterMarketOffers.allOffers"), Filters.Offers.ALL);
        myOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterMarketOffers.myOffers"), Filters.Offers.MINE);
        buyOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterMarketOffers.buyOffers"), Filters.Offers.BUY);
        sellOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterMarketOffers.sellOffers"), Filters.Offers.SELL);
        dropdownMenu.addMenuItems(allOffers, myOffers, buyOffers, sellOffers);
        return dropdownMenu;
    }

    private void updateMarketSelectorHeaderIcon(Node node) {
        channelTitle.setGraphic(node);
    }

    private void updateSelectedMarketFilter(Filters.Markets marketFilter) {
        if (marketFilter == null) {
            return;
        }

        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof DropdownFilterMenuItem)
                .forEach(menuItem -> {
                    DropdownFilterMenuItem<?> filterMenuItem = (DropdownFilterMenuItem<?>) menuItem;
                    filterMenuItem.updateSelection(marketFilter == filterMenuItem.getFilter());
                });
    }

    private void updateSelectedOffersFilter(Filters.Offers offersFilter) {
        if (offersFilter == null) {
            return;
        }

        offersFilterMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof DropdownFilterMenuItem)
                .forEach(menuItem -> {
                    DropdownFilterMenuItem<?> filterMenuItem = (DropdownFilterMenuItem<?>) menuItem;
                    filterMenuItem.updateSelection(offersFilter == filterMenuItem.getFilter());
                    if (offersFilter == filterMenuItem.getFilter()) {
                        offersFilterMenu.setLabel(((DropdownFilterMenuItem<?>) menuItem).getLabelText());
                    }
                });
    }

    private static final class DropdownFilterMenuItem<T> extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
        @Getter
        private final Filters.FilterPredicate<T> filter;

        DropdownFilterMenuItem(String defaultIconId, String activeIconId, String text, Filters.FilterPredicate<T> filter) {
            super(defaultIconId, activeIconId, text);

            this.filter = filter;
            getStyleClass().add("dropdown-filter-menu-item");
            updateSelection(false);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }
    }

    private static final class DropdownSortByMenuItem extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
        @Getter
        private final Comparator<MarketChannelItem> comparator;

        DropdownSortByMenuItem(String defaultIconId, String activeIconId, String text,
                                      Comparator<MarketChannelItem> comparator) {
            super(defaultIconId, activeIconId, text);

            this.comparator = comparator;
            getStyleClass().add("dropdown-sort-by-menu-item");
            updateSelection(false);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }

        boolean isSelected() {
            return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
        }
    }
}
