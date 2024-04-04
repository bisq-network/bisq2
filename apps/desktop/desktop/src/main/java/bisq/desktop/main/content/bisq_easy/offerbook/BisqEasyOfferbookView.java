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
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.controls.DropdownTitleMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
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
    private final ListChangeListener<MarketChannelItem> listChangeListener;
    private SearchBox marketSelectorSearchBox;
    private BisqTableView<MarketChannelItem> marketsTableView, favouritesTableView;
    private VBox marketSelectionList;
    private Subscription marketsTableViewSelectionPin, selectedModelItemPin, channelHeaderIconPin, selectedMarketFilterPin,
            selectedOfferDirectionOrOwnerFilterPin, selectedPeerReputationFilterPin, selectedMarketSortTypePin,
            marketSelectorSearchPin, favouritesTableViewHeightPin, favouritesTableViewSelectionPin,
            shouldShowAppliedFiltersPin;
    private Button createOfferButton;
    private DropdownMenu sortAndFilterMarketsMenu, filterOffersByDirectionOrOwnerMenu, filterOffersByPeerReputationMenu;
    private DropdownSortByMenuItem sortByMostOffers, sortByNameAZ, sortByNameZA;
    private DropdownFilterMenuItem<MarketChannelItem> filterShowAll, filterWithOffers, filterFavourites;
    private DropdownFilterMenuItem<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>
            allOffers, myOffers, buyOffers, sellOffers, allReputations, fiveStars, atLeastFourStars, atLeastThreeStars,
            atLeastTwoStars, atLeastOneStar;
    private DropdownTitleMenuItem atLeastTitle;
    private CheckBox hideUserMessagesCheckbox;
    private Label channelHeaderIcon, marketPrice, removeWithOffersFilter, removeFavouritesFilter;
    private HBox appliedFiltersSection, withOffersDisplayHint, onlyFavouritesDisplayHint;
    private ImageView withOffersRemoveFilterDefaultIcon, withOffersRemoveFilterActiveIcon,
            favouritesRemoveFilterDefaultIcon, favouritesRemoveFilterActiveIcon;

    public BisqEasyOfferbookView(BisqEasyOfferbookModel model,
                                 BisqEasyOfferbookController controller,
                                 VBox chatMessagesComponent,
                                 Pane channelSidebar) {
        super(model, controller, chatMessagesComponent, channelSidebar);

        listChangeListener = change -> updateTableViewSelection(getModel().getSelectedMarketChannelItem().get());
    }

    @Override
    protected void configTitleHBox() {
        super.configTitleHBox();

        marketPrice = new Label();
        HBox marketDescription = new HBox(5, channelDescription, marketPrice);
        channelDescription.getStyleClass().add("offerbook-channel-market-code");
        marketPrice.getStyleClass().addAll("chat-header-description", "offerbook-channel-market-price");

        VBox titleAndDescription = new VBox(channelTitle, marketDescription);
        channelTitle.getStyleClass().add("offerbook-channel-title");

        channelHeaderIcon = new Label();
        HBox headerTitle = new HBox(10, channelHeaderIcon, titleAndDescription);
        headerTitle.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        createOfferButton = createAndGetCreateOfferButton();
        titleHBox.getChildren().setAll(headerTitle, createOfferButton, headerDropdownMenu);
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
        marketPrice.textProperty().bind(getModel().getMarketPrice());
        withOffersDisplayHint.visibleProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(Filters.Markets.WITH_OFFERS));
        withOffersDisplayHint.managedProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(Filters.Markets.WITH_OFFERS));
        onlyFavouritesDisplayHint.visibleProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(Filters.Markets.FAVOURITES));
        onlyFavouritesDisplayHint.managedProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(Filters.Markets.FAVOURITES));
        favouritesTableView.visibleProperty().bind(Bindings.isNotEmpty(getModel().getFavouriteMarketChannelItems()));
        favouritesTableView.managedProperty().bind(Bindings.isNotEmpty(getModel().getFavouriteMarketChannelItems()));

        selectedModelItemPin = EasyBind.subscribe(getModel().getSelectedMarketChannelItem(), this::updateTableViewSelection);
        marketsTableViewSelectionPin = EasyBind.subscribe(marketsTableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectMarketChannelItem(item);
            }
        });
        marketSelectorSearchPin = EasyBind.subscribe(getModel().getMarketSelectorSearchText(), searchText -> {
            marketsTableView.getSelectionModel().select(getModel().getSelectedMarketChannelItem().get());
        });
        favouritesTableViewSelectionPin = EasyBind.subscribe(favouritesTableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectMarketChannelItem(item);
            }
        });
        getModel().getFavouriteMarketChannelItems().addListener(listChangeListener);

        channelHeaderIconPin = EasyBind.subscribe(model.getChannelIconNode(), this::updateChannelHeaderIcon);
        selectedMarketFilterPin = EasyBind.subscribe(getModel().getSelectedMarketsFilter(), this::updateSelectedMarketFilter);
        selectedOfferDirectionOrOwnerFilterPin = EasyBind.subscribe(getModel().getSelectedOfferDirectionOrOwnerFilter(), filter ->
                updateSelectedFilterInDropdownMenu(filter, filterOffersByDirectionOrOwnerMenu));
        selectedPeerReputationFilterPin = EasyBind.subscribe(getModel().getSelectedPeerReputationFilter(), filter ->
                updateSelectedFilterInDropdownMenu(filter, filterOffersByPeerReputationMenu));
        selectedMarketSortTypePin = EasyBind.subscribe(getModel().getSelectedMarketSortType(), this::updateMarketSortType);
        favouritesTableViewHeightPin = EasyBind.subscribe(getModel().getFavouritesTableViewHeight(),
                height -> updateFavouritesTableViewHeight(height.doubleValue()));
        shouldShowAppliedFiltersPin = EasyBind.subscribe(getModel().getShouldShowAppliedFilters(),
                this::updateAppliedFiltersSectionStyles);

        sortByMostOffers.setOnAction(e -> getController().onSortMarkets(MarketSortType.NUM_OFFERS));
        sortByNameAZ.setOnAction(e -> getController().onSortMarkets(MarketSortType.ASC));
        sortByNameZA.setOnAction(e -> getController().onSortMarkets(MarketSortType.DESC));

        filterWithOffers.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.WITH_OFFERS));
        filterShowAll.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.ALL));
        filterFavourites.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.FAVOURITES));

        allOffers.setOnAction(e -> setOfferDirectionOrOwnerFilter(allOffers));
        myOffers.setOnAction(e -> setOfferDirectionOrOwnerFilter(myOffers));
        buyOffers.setOnAction(e -> setOfferDirectionOrOwnerFilter(buyOffers));
        sellOffers.setOnAction(e -> setOfferDirectionOrOwnerFilter(sellOffers));

        allReputations.setOnAction(e -> setPeerReputationFilter(allReputations));
        fiveStars.setOnAction(e -> setPeerReputationFilter(fiveStars));
        atLeastFourStars.setOnAction(e -> setPeerReputationFilter(atLeastFourStars));
        atLeastThreeStars.setOnAction(e -> setPeerReputationFilter(atLeastThreeStars));
        atLeastTwoStars.setOnAction(e -> setPeerReputationFilter(atLeastTwoStars));
        atLeastOneStar.setOnAction(e -> setPeerReputationFilter(atLeastOneStar));

        createOfferButton.setOnAction(e -> getController().onCreateOffer());

        removeWithOffersFilter.setOnMouseClicked(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.ALL));
        withOffersDisplayHint.setOnMouseEntered(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterActiveIcon));
        withOffersDisplayHint.setOnMouseExited(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterDefaultIcon));

        removeFavouritesFilter.setOnMouseClicked(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.ALL));
        onlyFavouritesDisplayHint.setOnMouseEntered(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterActiveIcon));
        onlyFavouritesDisplayHint.setOnMouseExited(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterDefaultIcon));
    }

    private void updateTableViewSelection(MarketChannelItem selectedItem) {
        marketsTableView.getSelectionModel().clearSelection();
        marketsTableView.getSelectionModel().select(selectedItem);
        favouritesTableView.getSelectionModel().clearSelection();
        favouritesTableView.getSelectionModel().select(selectedItem);
    }

    private void updateFavouritesTableViewHeight(double height) {
        favouritesTableView.setMinHeight(height);
        favouritesTableView.setPrefHeight(height);
        favouritesTableView.setMaxHeight(height);
    }

    private void setOfferDirectionOrOwnerFilter(DropdownFilterMenuItem<?> filterMenuItem) {
        getModel().getSelectedOfferDirectionOrOwnerFilter().set((Filters.OfferDirectionOrOwner) filterMenuItem.getFilter());
    }

    private void setPeerReputationFilter(DropdownFilterMenuItem<?> filterMenuItem) {
        getModel().getSelectedPeerReputationFilter().set((Filters.PeerReputation) filterMenuItem.getFilter());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        hideUserMessagesCheckbox.selectedProperty().unbindBidirectional(getModel().getOfferOnly());
        marketSelectorSearchBox.textProperty().unbindBidirectional(getModel().getMarketSelectorSearchText());
        marketPrice.textProperty().unbind();
        withOffersDisplayHint.visibleProperty().unbind();
        withOffersDisplayHint.managedProperty().unbind();
        onlyFavouritesDisplayHint.visibleProperty().unbind();
        onlyFavouritesDisplayHint.managedProperty().unbind();
        favouritesTableView.visibleProperty().unbind();
        favouritesTableView.managedProperty().unbind();

        selectedModelItemPin.unsubscribe();
        marketsTableViewSelectionPin.unsubscribe();
        marketSelectorSearchPin.unsubscribe();
        favouritesTableViewSelectionPin.unsubscribe();
        channelHeaderIconPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedOfferDirectionOrOwnerFilterPin.unsubscribe();
        selectedPeerReputationFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();
        favouritesTableViewHeightPin.unsubscribe();
        shouldShowAppliedFiltersPin.unsubscribe();

        sortByMostOffers.setOnAction(null);
        sortByNameAZ.setOnAction(null);
        sortByNameZA.setOnAction(null);
        filterWithOffers.setOnAction(null);
        filterShowAll.setOnAction(null);
        filterFavourites.setOnAction(null);
        allOffers.setOnAction(null);
        myOffers.setOnAction(null);
        buyOffers.setOnAction(null);
        sellOffers.setOnAction(null);
        allReputations.setOnAction(null);
        fiveStars.setOnAction(null);
        atLeastFourStars.setOnAction(null);
        atLeastThreeStars.setOnAction(null);
        atLeastTwoStars.setOnAction(null);
        atLeastOneStar.setOnAction(null);
        createOfferButton.setOnAction(null);

        removeWithOffersFilter.setOnMouseClicked(null);
        withOffersDisplayHint.setOnMouseEntered(null);
        withOffersDisplayHint.setOnMouseExited(null);

        removeFavouritesFilter.setOnMouseClicked(null);
        onlyFavouritesDisplayHint.setOnMouseEntered(null);
        onlyFavouritesDisplayHint.setOnMouseExited(null);

        getModel().getFavouriteMarketChannelItems().removeListener(listChangeListener);
    }

    private BisqEasyOfferbookModel getModel() {
        return (BisqEasyOfferbookModel) model;
    }

    private BisqEasyOfferbookController getController() {
        return (BisqEasyOfferbookController) controller;
    }

    private void addMarketSelectionList() {
        Label marketSelectionTitle = new Label(Res.get("bisqEasy.offerbook.markets"));
        HBox header = new HBox(marketSelectionTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 0, 15));
        header.getStyleClass().add("chat-header-title");

        marketSelectorSearchBox = new SearchBox();
        marketSelectorSearchBox.getStyleClass().add("offerbook-search-box");
        sortAndFilterMarketsMenu = createAndGetSortAndFilterMarketsMenu();
        HBox subheader = new HBox(marketSelectorSearchBox, Spacer.fillHBox(), sortAndFilterMarketsMenu);
        subheader.setAlignment(Pos.CENTER);
        subheader.getStyleClass().add("market-selection-subheader");

        withOffersRemoveFilterDefaultIcon = ImageUtil.getImageViewById("close-mini-grey");
        withOffersRemoveFilterActiveIcon = ImageUtil.getImageViewById("close-mini-white");
        removeWithOffersFilter = createAndGetRemoveFilterLabel(withOffersRemoveFilterDefaultIcon);
        withOffersDisplayHint = createAndGetDisplayHintHBox(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), removeWithOffersFilter);

        favouritesRemoveFilterDefaultIcon = ImageUtil.getImageViewById("close-mini-grey");
        favouritesRemoveFilterActiveIcon = ImageUtil.getImageViewById("close-mini-white");
        removeFavouritesFilter = createAndGetRemoveFilterLabel(favouritesRemoveFilterDefaultIcon);
        onlyFavouritesDisplayHint = createAndGetDisplayHintHBox(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites"), removeFavouritesFilter);

        appliedFiltersSection = new HBox(withOffersDisplayHint, onlyFavouritesDisplayHint);
        appliedFiltersSection.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(appliedFiltersSection, Priority.ALWAYS);

        favouritesTableView = new BisqTableView<>(getModel().getFavouriteMarketChannelItems());
        favouritesTableView.getStyleClass().addAll("market-selection-list", "favourites-list");
        favouritesTableView.hideVerticalScrollbar();
        favouritesTableView.hideHorizontalScrollbar();
        favouritesTableView.setFixedCellSize(getController().getMarketSelectionListCellHeight());
        configTableView(favouritesTableView);

        marketsTableView = new BisqTableView<>(getModel().getSortedMarketChannelItems());
        marketsTableView.getStyleClass().addAll("market-selection-list", "markets-list");
        marketsTableView.allowVerticalScrollbar();
        marketsTableView.hideHorizontalScrollbar();
        marketsTableView.setFixedCellSize(getController().getMarketSelectionListCellHeight());
        marketsTableView.setPlaceholder(new Label());
        configTableView(marketsTableView);
        VBox.setVgrow(marketsTableView, Priority.ALWAYS);

        marketSelectionList = new VBox(header, Layout.hLine(), subheader, appliedFiltersSection, favouritesTableView,
                marketsTableView);
        marketSelectionList.setPrefWidth(210);
        marketSelectionList.setMinWidth(210);
        marketSelectionList.setFillWidth(true);
        marketSelectionList.getStyleClass().add("chat-container");
    }

    private Label createAndGetRemoveFilterLabel(ImageView defaultCloseIcon) {
        Label removeFilterLabel = new Label();
        removeFilterLabel.setGraphic(defaultCloseIcon);
        removeFilterLabel.setCursor(Cursor.HAND);
        return removeFilterLabel;
    }

    private HBox createAndGetDisplayHintHBox(String labelText, Label removeFilter) {
        Label label = new Label(labelText);
        label.getStyleClass().add("small-text");
        HBox displayHintHBox = new HBox(5, label, removeFilter);
        displayHintHBox.setAlignment(Pos.CENTER);
        displayHintHBox.getStyleClass().add("filter-display-hint");
        return displayHintHBox;
    }

    private DropdownMenu createAndGetSortAndFilterMarketsMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("sort-grey", "sort-white", true);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.tooltip"));
        dropdownMenu.getStyleClass().add("market-selection-dropdown-menu");

        // Sorting options
        DropdownTitleMenuItem sortTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.sortTitle"));
        sortByMostOffers = new DropdownSortByMenuItem("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.mostOffers"),
                MarketSortType.NUM_OFFERS);
        sortByNameAZ = new DropdownSortByMenuItem("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameAZ"),
                MarketSortType.ASC);
        sortByNameZA = new DropdownSortByMenuItem("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameZA"),
                MarketSortType.DESC);

        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Filter options
        DropdownTitleMenuItem filterTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.filterTitle"));
        filterWithOffers = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), Filters.Markets.WITH_OFFERS);
        filterFavourites = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites"), Filters.Markets.FAVOURITES);
        filterShowAll = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.all"), Filters.Markets.ALL);

        dropdownMenu.addMenuItems(sortTitle, sortByMostOffers, sortByNameAZ, sortByNameZA, separator, filterTitle,
                filterWithOffers, filterFavourites, filterShowAll);
        return dropdownMenu;
    }

    private Button createAndGetCreateOfferButton() {
        Button createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        return createOfferButton;
    }

    private void configTableView(BisqTableView<MarketChannelItem> tableView) {
        BisqTableColumn<MarketChannelItem> marketLogoTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .fixWidth(55)
                .setCellFactory(BisqEasyOfferbookUtil.getMarketLogoCellFactory())
                .isSortable(false)
                .build();

        BisqTableColumn<MarketChannelItem> marketLabelTableColumn = new BisqTableColumn.Builder<MarketChannelItem>()
                .minWidth(100)
                .left()
                .setCellFactory(BisqEasyOfferbookUtil.getMarketLabelCellFactory(tableView.equals(favouritesTableView)))
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

        filterOffersByPeerReputationMenu = createAndGetPeerReputationFilterMenu();
        filterOffersByDirectionOrOwnerMenu = createAndGetOfferDirectionOrOwnerFilterMenu();

        searchBox.getStyleClass().add("offerbook-search-box");
        HBox subheaderContent = new HBox(30, searchBox, Spacer.fillHBox(), checkbox,
                filterOffersByPeerReputationMenu, filterOffersByDirectionOrOwnerMenu);
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

    private DropdownMenu createAndGetOfferDirectionOrOwnerFilterMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allOffers = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.allOffers"), Filters.OfferDirectionOrOwner.ALL);
        myOffers = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.myOffers"), Filters.OfferDirectionOrOwner.MINE);
        buyOffers = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.buyOffers"), Filters.OfferDirectionOrOwner.BUY);
        sellOffers = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.sellOffers"), Filters.OfferDirectionOrOwner.SELL);

        dropdownMenu.addMenuItems(sellOffers, buyOffers, myOffers, allOffers);
        return dropdownMenu;
    }

    private DropdownMenu createAndGetPeerReputationFilterMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allReputations = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.allReputations"),
                Filters.PeerReputation.ALL);
        fiveStars = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.fiveStars"),
                Filters.PeerReputation.FIVE_STARS);
        atLeastTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastTitle"));
        atLeastFourStars = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastFourStars"),
                Filters.PeerReputation.AT_LEAST_FOUR_STARS);
        atLeastThreeStars = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastThreeStars"),
                Filters.PeerReputation.AT_LEAST_THREE_STARS);
        atLeastTwoStars = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastTwoStars"),
                Filters.PeerReputation.AT_LEAST_TWO_STARS);
        atLeastOneStar = new DropdownFilterMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastOneStar"),
                Filters.PeerReputation.AT_LEAST_ONE_STAR);

        dropdownMenu.addMenuItems(fiveStars, atLeastTitle, atLeastFourStars, atLeastThreeStars, atLeastTwoStars,
                atLeastOneStar, allReputations);
        return dropdownMenu;
    }

    private void updateChannelHeaderIcon(Node node) {
        channelHeaderIcon.setGraphic(node);
    }

    private void updateSelectedMarketFilter(Filters.Markets marketFilter) {
        if (marketFilter == null) {
            return;
        }

        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof DropdownFilterMenuItem)
                .map(menuItem -> (DropdownFilterMenuItem<?>) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(marketFilter == menuItem.getFilter()));

        marketsTableView.getSelectionModel().select(getModel().getSelectedMarketChannelItem().get());
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
                        String menuItemLabel = ((DropdownFilterMenuItem<?>) menuItem).getLabelText();
                        if (selectedFilter instanceof Filters.PeerReputation) {
                            menuItemLabel = createPeerReputationLabel((Filters.PeerReputation) selectedFilter, menuItemLabel);
                        }
                        dropdownMenu.setLabel(menuItemLabel);
                    }
                });
    }

    private void updateAppliedFiltersSectionStyles(boolean shouldShowAppliedFilters) {
        appliedFiltersSection.getStyleClass().clear();
        appliedFiltersSection.getStyleClass().add(shouldShowAppliedFilters
                ? "market-selection-show-applied-filters"
                : "market-selection-no-filters");
    }

    private String createPeerReputationLabel(Filters.PeerReputation filter, String label) {
        switch (filter) {
            case AT_LEAST_FOUR_STARS:
            case AT_LEAST_THREE_STARS:
            case AT_LEAST_TWO_STARS:
            case AT_LEAST_ONE_STAR:
                return String.format("%s %s", atLeastTitle.getLabelText().replace(":", ""), label);
            case FIVE_STARS:
            case ALL:
            default:
                return label;
        }
    }

    private static final class DropdownFilterMenuItem<T> extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        @Getter
        private final Filters.FilterPredicate<T> filter;

        DropdownFilterMenuItem(String defaultIconId,
                               String activeIconId,
                               String text,
                               Filters.FilterPredicate<T> filter) {
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
