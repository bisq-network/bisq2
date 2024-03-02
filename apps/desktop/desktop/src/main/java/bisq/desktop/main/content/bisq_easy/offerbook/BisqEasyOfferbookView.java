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

@Slf4j
public final class BisqEasyOfferbookView extends ChatView<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private SearchBox marketSelectorSearchBox;
    private BisqTableView<MarketChannelItem> tableView;
    private VBox marketSelectionList;
    private Subscription tableViewSelectionPin, selectedModelItemPin, marketSelectorHeaderIconPin,
            selectedMarketFilterPin, selectedOfferTypeFilterPin, selectedOfferReputationsFilterPin, selectedMarketSortTypePin;
    private Button createOfferButton;
    private DropdownMenu sortAndFilterMarketsMenu, filterOffersByTypeMenu, filterOffersByReputationMenu;
    private DropdownSortByMenuItem sortByMostOffers, sortByNameAZ, sortByNameZA;
    private DropdownFilterMenuItem<MarketChannelItem> filterShowAll, filterWithOffers;
    private DropdownFilterMenuItem<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>
            allOffers, myOffers, buyOffers, sellOffers, allReputations, fiveStars, atLeastFourStars, atLeastThreeStars,
            atLeastTwoStars, atLeastOneStar;
    private CheckBox hideUserMessagesCheckbox;

    public BisqEasyOfferbookView(BisqEasyOfferbookModel model,
                                 BisqEasyOfferbookController controller,
                                 VBox chatMessagesComponent,
                                 Pane channelSidebar) {
        super(model, controller, chatMessagesComponent, channelSidebar);

    }

    @Override
    protected void configTitleHBox() {
        super.configTitleHBox();

        Label chatDomainTitle = new Label(Res.get("bisqEasy.offerbook"));
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

        hideUserMessagesCheckbox.selectedProperty().bindBidirectional(getModel().getOfferOnly());
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
        selectedOfferTypeFilterPin = EasyBind.subscribe(getModel().getSelectedOfferTypeFilter(), filter ->
                updateSelectedFilterInDropdownMenu(filter, filterOffersByTypeMenu));
        selectedOfferReputationsFilterPin = EasyBind.subscribe(getModel().getSelectedReputationsFilter(), filter ->
                updateSelectedFilterInDropdownMenu(filter, filterOffersByReputationMenu));
        selectedMarketSortTypePin = EasyBind.subscribe(getModel().getSelectedMarketSortType(), this::updateMarketSortType);

        sortByMostOffers.setOnAction(e -> getController().onSortMarkets(MarketSortType.NUM_OFFERS));
        sortByNameAZ.setOnAction(e -> getController().onSortMarkets(MarketSortType.ASC));
        sortByNameZA.setOnAction(e -> getController().onSortMarkets(MarketSortType.DESC));

        filterWithOffers.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.WITH_OFFERS));
        filterShowAll.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.ALL));

        allOffers.setOnAction(e -> setOffersFilter(allOffers));
        myOffers.setOnAction(e -> setOffersFilter(myOffers));
        buyOffers.setOnAction(e -> setOffersFilter(buyOffers));
        sellOffers.setOnAction(e -> setOffersFilter(sellOffers));

        allReputations.setOnAction(e -> setReputationsFilter(allReputations));
        fiveStars.setOnAction(e -> setReputationsFilter(fiveStars));
        atLeastFourStars.setOnAction(e -> setReputationsFilter(atLeastFourStars));
        atLeastThreeStars.setOnAction(e -> setReputationsFilter(atLeastThreeStars));
        atLeastTwoStars.setOnAction(e -> setReputationsFilter(atLeastTwoStars));
        atLeastOneStar.setOnAction(e -> setReputationsFilter(atLeastOneStar));

        createOfferButton.setOnAction(e -> getController().onCreateOffer());
    }

    private void setOffersFilter(DropdownFilterMenuItem<?> filterMenuItem) {
        getModel().getSelectedOfferTypeFilter().set((Filters.OfferType) filterMenuItem.getFilter());
    }

    private void setReputationsFilter(DropdownFilterMenuItem<?> filterMenuItem) {
        getModel().getSelectedReputationsFilter().set((Filters.OfferReputations) filterMenuItem.getFilter());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        hideUserMessagesCheckbox.selectedProperty().unbindBidirectional(getModel().getOfferOnly());
        marketSelectorSearchBox.textProperty().unbindBidirectional(getModel().getMarketSelectorSearchText());

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        marketSelectorHeaderIconPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedOfferTypeFilterPin.unsubscribe();
        selectedOfferReputationsFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();

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
        sortByMostOffers = new DropdownSortByMenuItem("check-grey",
                "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.mostOffers"),
                MarketSortType.NUM_OFFERS);
        sortByNameAZ = new DropdownSortByMenuItem("check-grey",
                "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameAZ"),
                MarketSortType.ASC);
        sortByNameZA = new DropdownSortByMenuItem("check-grey",
                "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameZA"),
                MarketSortType.DESC);

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

        BisqTableColumn<MarketChannelItem> marketLabelTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
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

        filterOffersByReputationMenu = createAndGetReputationFilterMenu();
        filterOffersByTypeMenu = createAndGetOffersFilterMenu();
        
        HBox subheaderContent = new HBox(30, checkbox, filterOffersByReputationMenu, filterOffersByTypeMenu);
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
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByType.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByType.allOffers"), Filters.OfferType.ALL);
        myOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByType.myOffers"), Filters.OfferType.MINE);
        buyOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByType.buyOffers"), Filters.OfferType.BUY);
        sellOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByType.sellOffers"), Filters.OfferType.SELL);
        dropdownMenu.addMenuItems(allOffers, myOffers, buyOffers, sellOffers);
        return dropdownMenu;
    }

    private DropdownMenu createAndGetReputationFilterMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("arrow-down", "arrow-down", false);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allReputations = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.allReputations"), Filters.OfferReputations.ALL);
        fiveStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.fiveStars"), Filters.OfferReputations.FIVE_STARS);
        atLeastFourStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.atLeastFourStars"),
                Filters.OfferReputations.AT_LEAST_FOUR_STARS);
        atLeastThreeStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.atLeastThreeStars"),
                Filters.OfferReputations.AT_LEAST_THREE_STARS);
        atLeastTwoStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.atLeastTwoStars"),
                Filters.OfferReputations.AT_LEAST_TWO_STARS);
        atLeastOneStar = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByReputation.atLeastOneStar"),
                Filters.OfferReputations.AT_LEAST_ONE_STAR);
        dropdownMenu.addMenuItems(allReputations, fiveStars, atLeastFourStars, atLeastThreeStars, atLeastTwoStars,
                atLeastOneStar);
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
                .map(menuItem -> (DropdownFilterMenuItem<?>) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(marketFilter == menuItem.getFilter()));
    }

    private void updateMarketSortType(MarketSortType marketSortType) {
        if (marketSortType == null) {
            return;
        }

        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof DropdownSortByMenuItem)
                .map(menuItem -> (DropdownSortByMenuItem) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(marketSortType == menuItem.marketSortType));
    }

    private <T> void updateSelectedFilterInDropdownMenu(T selectedFilter, DropdownMenu dropdownMenu) {
        if (selectedFilter == null) {
            return;
        }

        dropdownMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof DropdownFilterMenuItem)
                .forEach(menuItem -> {
                    DropdownFilterMenuItem<?> filterMenuItem = (DropdownFilterMenuItem<?>) menuItem;
                    filterMenuItem.updateSelection(selectedFilter == filterMenuItem.getFilter());
                    if (selectedFilter == filterMenuItem.getFilter()) {
                        dropdownMenu.setLabel(((DropdownFilterMenuItem<?>) menuItem).getLabelText());
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
        private final MarketSortType marketSortType;

        DropdownSortByMenuItem(String defaultIconId,
                               String activeIconId,
                               String text,
                               MarketSortType marketSortType) {
            super(defaultIconId, activeIconId, text);

            this.marketSortType = marketSortType;
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
