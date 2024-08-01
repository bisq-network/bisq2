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

import bisq.bisq_easy.BisqEasyMarketFilter;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

import static bisq.bisq_easy.BisqEasyMarketFilter.*;

@Slf4j
public final class BisqEasyOfferbookView extends ChatView<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private static final String BUY_FROM_MENU_ITEM_STYLE_CLASS = "buy-from-offers";
    private static final String SELL_TO_MENU_ITEM_STYLE_CLASS = "sell-to-offers";
    private static final double EXPANDED_OFFER_LIST_WIDTH = 438;
    private static final double EXPANDED_MARKET_SELECTION_LIST_WIDTH = 210;
    private static final double COLLAPSED_LIST_WIDTH = 40;

    private final ListChangeListener<MarketChannelItem> listChangeListener;
    private SearchBox marketSelectorSearchBox;
    private BisqTableView<MarketChannelItem> marketsTableView, favouritesTableView;
    private BisqTableView<OfferMessageItem> offerListTableView;
    private VBox marketSelectionList, collapsedMarketSelectionList, collapsedOfferList, offerList;
    private Subscription marketsTableViewSelectionPin, selectedModelItemPin, channelHeaderIconPin, selectedMarketFilterPin,
            selectedMarketSortTypePin, marketSelectorSearchPin, favouritesTableViewHeightPin,
            favouritesTableViewSelectionPin, shouldShowAppliedFiltersPin, offerListTableViewSelectionPin,
            showBuyFromOffersPin, showOfferListExpandedPin, showMarketSelectionListCollapsedPin;
    private Button createOfferButton;
    private DropdownMenu sortAndFilterMarketsMenu;
    private SortAndFilterMarketsDropdownMenuItem<MarketSortType> sortByMostOffers, sortByNameAZ, sortByNameZA;
    private SortAndFilterMarketsDropdownMenuItem<BisqEasyMarketFilter> filterShowAll, filterWithOffers, filterFavourites;
    private DropdownMenuItem buyFromOffers, sellToOffers;
    private Label channelHeaderIcon, marketPrice, removeWithOffersFilter, removeFavouritesFilter,
            offerListByDirectionFilter, collapsedOfferListTitle, offerListTitle, collapsedMarketSelectionListTitle,
            marketSelectionListTitle;
    private HBox appliedFiltersSection, withOffersDisplayHint, onlyFavouritesDisplayHint;
    private ImageView withOffersRemoveFilterDefaultIcon, withOffersRemoveFilterActiveIcon,
            favouritesRemoveFilterDefaultIcon, favouritesRemoveFilterActiveIcon, marketsGreenIcon, marketsGreyIcon,
            marketsWhiteIcon, offerListGreenIcon, offerListGreyIcon, offerListWhiteIcon;

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

        createOfferButton = createAndGetCreateOfferButton();
        HBox.setHgrow(createOfferButton, Priority.ALWAYS);
        titleHBox.getChildren().setAll(headerTitle, Spacer.fillHBox(), createOfferButton, headerDropdownMenu);
    }

    @Override
    protected void configCenterVBox() {
        addMarketSelectionList();
        addCollapsedMarketSelectionList();
        addChatBox();
        addCollapsedOfferList();
        addOfferList();
    }

    @Override
    protected void configContainerHBox() {
        super.configContainerHBox();

        containerHBox.getChildren().setAll(marketSelectionList, collapsedMarketSelectionList, centerVBox, collapsedOfferList, offerList, sideBar);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        favouritesTableView.initialize();
        marketsTableView.initialize();

        marketSelectorSearchBox.textProperty().bindBidirectional(getModel().getMarketSelectorSearchText());
        marketPrice.textProperty().bind(getModel().getMarketPrice());
        withOffersDisplayHint.visibleProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(WITH_OFFERS));
        withOffersDisplayHint.managedProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(WITH_OFFERS));
        onlyFavouritesDisplayHint.visibleProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(FAVOURITES));
        onlyFavouritesDisplayHint.managedProperty().bind(getModel().getSelectedMarketsFilter().isEqualTo(FAVOURITES));
        favouritesTableView.visibleProperty().bind(Bindings.isNotEmpty(getModel().getFavouriteMarketChannelItems()));
        favouritesTableView.managedProperty().bind(Bindings.isNotEmpty(getModel().getFavouriteMarketChannelItems()));
        collapsedOfferList.visibleProperty().bind(getModel().getShowOfferListExpanded().not());
        collapsedOfferList.managedProperty().bind(getModel().getShowOfferListExpanded().not());
        offerList.visibleProperty().bind(getModel().getShowOfferListExpanded());
        offerList.managedProperty().bind(getModel().getShowOfferListExpanded());
        collapsedMarketSelectionList.visibleProperty().bind(getModel().getShowMarketSelectionListCollapsed());
        collapsedMarketSelectionList.managedProperty().bind(getModel().getShowMarketSelectionListCollapsed());
        marketSelectionList.visibleProperty().bind(getModel().getShowMarketSelectionListCollapsed().not());
        marketSelectionList.managedProperty().bind(getModel().getShowMarketSelectionListCollapsed().not());

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
        selectedMarketSortTypePin = EasyBind.subscribe(getModel().getSelectedMarketSortType(), this::updateMarketSortType);
        favouritesTableViewHeightPin = EasyBind.subscribe(getModel().getFavouritesTableViewHeight(),
                height -> updateFavouritesTableViewHeight(height.doubleValue()));
        shouldShowAppliedFiltersPin = EasyBind.subscribe(getModel().getShouldShowAppliedFilters(),
                this::updateAppliedFiltersSectionStyles);

        offerListTableViewSelectionPin = EasyBind.subscribe(offerListTableView.getSelectionModel().selectedItemProperty(), item -> {
            getController().onSelectOfferMessageItem(item);
        });
        showBuyFromOffersPin = EasyBind.subscribe(getModel().getShowBuyOffers(), showBuyFromOffers -> {
            updateOfferListByDirectionFilter();
        });

        showOfferListExpandedPin = EasyBind.subscribe(getModel().getShowOfferListExpanded(),
                showOfferListExpanded -> updateChatContainerStyleClass());

        showMarketSelectionListCollapsedPin = EasyBind.subscribe(getModel().getShowMarketSelectionListCollapsed(),
                showMarketSelectionListCollapsed -> updateChatContainerStyleClass());

        sortByMostOffers.setOnAction(e -> getController().onSortMarkets(MarketSortType.NUM_OFFERS));
        sortByNameAZ.setOnAction(e -> getController().onSortMarkets(MarketSortType.ASC));
        sortByNameZA.setOnAction(e -> getController().onSortMarkets(MarketSortType.DESC));

        filterWithOffers.setOnAction(e -> getModel().getSelectedMarketsFilter().set(WITH_OFFERS));
        filterShowAll.setOnAction(e -> getModel().getSelectedMarketsFilter().set(ALL));
        filterFavourites.setOnAction(e -> getModel().getSelectedMarketsFilter().set(FAVOURITES));

        createOfferButton.setOnAction(e -> getController().onCreateOffer());

        buyFromOffers.setOnAction(e -> getController().onSelectBuyFromFilter());
        sellToOffers.setOnAction(e -> getController().onSelectSellToFilter());

        removeWithOffersFilter.setOnMouseClicked(e -> getModel().getSelectedMarketsFilter().set(ALL));
        withOffersDisplayHint.setOnMouseEntered(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterActiveIcon));
        withOffersDisplayHint.setOnMouseExited(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterDefaultIcon));

        removeFavouritesFilter.setOnMouseClicked(e -> getModel().getSelectedMarketsFilter().set(ALL));
        onlyFavouritesDisplayHint.setOnMouseEntered(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterActiveIcon));
        onlyFavouritesDisplayHint.setOnMouseExited(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterDefaultIcon));

        offerListTitle.setOnMouseClicked(e ->
                Transitions.expansionAnimation(offerList, EXPANDED_OFFER_LIST_WIDTH, COLLAPSED_LIST_WIDTH + 20,
                        () -> getController().toggleOfferList()));
        offerListTitle.setOnMouseEntered(e -> offerListTitle.setGraphic(offerListWhiteIcon));
        offerListTitle.setOnMouseExited(e -> offerListTitle.setGraphic(offerListGreenIcon));

        collapsedOfferListTitle.setOnMouseClicked(e -> {
            getController().toggleOfferList();
            Transitions.expansionAnimation(offerList, COLLAPSED_LIST_WIDTH + 20, EXPANDED_OFFER_LIST_WIDTH);
        });
        collapsedOfferListTitle.setOnMouseEntered(e -> collapsedOfferListTitle.setGraphic(offerListWhiteIcon));
        collapsedOfferListTitle.setOnMouseExited(e -> collapsedOfferListTitle.setGraphic(offerListGreyIcon));

        marketSelectionListTitle.setOnMouseClicked(e ->
                Transitions.expansionAnimation(marketSelectionList, EXPANDED_MARKET_SELECTION_LIST_WIDTH,
                        COLLAPSED_LIST_WIDTH, () -> getController().toggleMarketSelectionList()));
        marketSelectionListTitle.setOnMouseEntered(e -> marketSelectionListTitle.setGraphic(marketsWhiteIcon));
        marketSelectionListTitle.setOnMouseExited(e -> marketSelectionListTitle.setGraphic(marketsGreenIcon));

        collapsedMarketSelectionListTitle.setOnMouseClicked(e -> {
            getController().toggleMarketSelectionList();
            Transitions.expansionAnimation(marketSelectionList, COLLAPSED_LIST_WIDTH, EXPANDED_MARKET_SELECTION_LIST_WIDTH);
        });
        collapsedMarketSelectionListTitle.setOnMouseEntered(e -> collapsedMarketSelectionListTitle.setGraphic(marketsWhiteIcon));
        collapsedMarketSelectionListTitle.setOnMouseExited(e -> collapsedMarketSelectionListTitle.setGraphic(marketsGreyIcon));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        marketsTableView.dispose();
        favouritesTableView.dispose();

        marketSelectorSearchBox.textProperty().unbindBidirectional(getModel().getMarketSelectorSearchText());
        marketPrice.textProperty().unbind();
        withOffersDisplayHint.visibleProperty().unbind();
        withOffersDisplayHint.managedProperty().unbind();
        onlyFavouritesDisplayHint.visibleProperty().unbind();
        onlyFavouritesDisplayHint.managedProperty().unbind();
        favouritesTableView.visibleProperty().unbind();
        favouritesTableView.managedProperty().unbind();
        collapsedOfferList.visibleProperty().unbind();
        collapsedOfferList.managedProperty().unbind();
        offerList.visibleProperty().unbind();
        offerList.managedProperty().unbind();
        collapsedMarketSelectionList.visibleProperty().unbind();
        collapsedMarketSelectionList.managedProperty().unbind();
        marketSelectionList.visibleProperty().unbind();
        marketSelectionList.managedProperty().unbind();

        selectedModelItemPin.unsubscribe();
        marketsTableViewSelectionPin.unsubscribe();
        marketSelectorSearchPin.unsubscribe();
        favouritesTableViewSelectionPin.unsubscribe();
        channelHeaderIconPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();
        favouritesTableViewHeightPin.unsubscribe();
        shouldShowAppliedFiltersPin.unsubscribe();
        offerListTableViewSelectionPin.unsubscribe();
        showBuyFromOffersPin.unsubscribe();
        showOfferListExpandedPin.unsubscribe();
        showMarketSelectionListCollapsedPin.unsubscribe();

        sortByMostOffers.setOnAction(null);
        sortByNameAZ.setOnAction(null);
        sortByNameZA.setOnAction(null);
        filterWithOffers.setOnAction(null);
        filterShowAll.setOnAction(null);
        filterFavourites.setOnAction(null);
        createOfferButton.setOnAction(null);
        buyFromOffers.setOnAction(null);
        sellToOffers.setOnAction(null);

        removeWithOffersFilter.setOnMouseClicked(null);
        withOffersDisplayHint.setOnMouseEntered(null);
        withOffersDisplayHint.setOnMouseExited(null);

        removeFavouritesFilter.setOnMouseClicked(null);
        onlyFavouritesDisplayHint.setOnMouseEntered(null);
        onlyFavouritesDisplayHint.setOnMouseExited(null);

        offerListTitle.setOnMouseClicked(null);
        offerListTitle.setOnMouseEntered(null);
        offerListTitle.setOnMouseExited(null);

        collapsedOfferListTitle.setOnMouseClicked(null);
        collapsedOfferListTitle.setOnMouseEntered(null);
        collapsedOfferListTitle.setOnMouseExited(null);

        marketSelectionListTitle.setOnMouseClicked(null);
        marketSelectionListTitle.setOnMouseEntered(null);
        marketSelectionListTitle.setOnMouseExited(null);

        collapsedMarketSelectionListTitle.setOnMouseClicked(null);
        collapsedMarketSelectionListTitle.setOnMouseEntered(null);
        collapsedMarketSelectionListTitle.setOnMouseExited(null);

        getModel().getFavouriteMarketChannelItems().removeListener(listChangeListener);
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

    private void updateChatContainerStyleClass() {
        centerVBox.getStyleClass().clear();
        String styleClass;
        boolean showMarketSelectionListCollapsed = getModel().getShowMarketSelectionListCollapsed().get();
        boolean showOfferListCollapsed = !getModel().getShowOfferListExpanded().get();
        if (showOfferListCollapsed && showMarketSelectionListCollapsed) {
            styleClass = "chat-container-with-both-lists-collapsed";
        } else if (showOfferListCollapsed) {
            styleClass = "chat-container-with-offer-list-collapsed";
        } else if (showMarketSelectionListCollapsed) {
            styleClass = "chat-container-with-market-selection-list-collapsed";
        } else { // both are expanded
            styleClass = "bisq-easy-container";
        }
        centerVBox.getStyleClass().add(styleClass);
    }

    private BisqEasyOfferbookModel getModel() {
        return (BisqEasyOfferbookModel) model;
    }

    private BisqEasyOfferbookController getController() {
        return (BisqEasyOfferbookController) controller;
    }

    private void addMarketSelectionList() {
        marketsGreenIcon = ImageUtil.getImageViewById("market-green");
        marketsGreyIcon = ImageUtil.getImageViewById("market-grey");
        marketsWhiteIcon = ImageUtil.getImageViewById("market-white");

        marketSelectionListTitle = new Label(Res.get("bisqEasy.offerbook.markets"), marketsGreenIcon);
        marketSelectionListTitle.setCursor(Cursor.HAND);
        marketSelectionListTitle.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.markets.ExpandedList.Tooltip")));
        HBox header = new HBox(marketSelectionListTitle);
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
        configMarketsTableView(favouritesTableView);

        marketsTableView = new BisqTableView<>(getModel().getSortedMarketChannelItems(), false);
        marketsTableView.getStyleClass().addAll("market-selection-list", "markets-list");
        marketsTableView.allowVerticalScrollbar();
        marketsTableView.hideHorizontalScrollbar();
        marketsTableView.setFixedCellSize(getController().getMarketSelectionListCellHeight());
        marketsTableView.setPlaceholder(new Label());
        configMarketsTableView(marketsTableView);
        VBox.setVgrow(marketsTableView, Priority.ALWAYS);

        marketSelectionList = new VBox(header, Layout.hLine(), subheader, appliedFiltersSection, favouritesTableView,
                marketsTableView);
        marketSelectionList.setMaxWidth(EXPANDED_MARKET_SELECTION_LIST_WIDTH);
        marketSelectionList.setPrefWidth(EXPANDED_MARKET_SELECTION_LIST_WIDTH);
        marketSelectionList.setMinWidth(EXPANDED_MARKET_SELECTION_LIST_WIDTH);
        marketSelectionList.setFillWidth(true);
        marketSelectionList.getStyleClass().add("chat-container");
    }

    private void addCollapsedMarketSelectionList() {
        collapsedMarketSelectionListTitle = new Label("", marketsGreyIcon);
        collapsedMarketSelectionListTitle.setCursor(Cursor.HAND);
        collapsedMarketSelectionListTitle.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.markets.CollapsedList.Tooltip")));
        HBox header = new HBox(collapsedMarketSelectionListTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(4, 0, 0, 0));

        HBox subheader = new HBox();
        subheader.setAlignment(Pos.CENTER);
        subheader.getStyleClass().add("market-selection-subheader");

        collapsedMarketSelectionList = new VBox(header, Layout.hLine(), subheader, Spacer.fillVBox());
        collapsedMarketSelectionList.setMaxWidth(COLLAPSED_LIST_WIDTH);
        collapsedMarketSelectionList.setPrefWidth(COLLAPSED_LIST_WIDTH);
        collapsedMarketSelectionList.setMinWidth(COLLAPSED_LIST_WIDTH);
        collapsedMarketSelectionList.setFillWidth(true);
        collapsedMarketSelectionList.getStyleClass().add("collapsed-market-selection-list-container");
        HBox.setMargin(collapsedMarketSelectionList, new Insets(0, -9, 0, 0));
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
        sortByMostOffers = new SortAndFilterMarketsDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.mostOffers"), MarketSortType.NUM_OFFERS);
        sortByNameAZ = new SortAndFilterMarketsDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameAZ"), MarketSortType.ASC);
        sortByNameZA = new SortAndFilterMarketsDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameZA"), MarketSortType.DESC);

        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Filter options
        DropdownTitleMenuItem filterTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.filterTitle"));
        filterWithOffers = new SortAndFilterMarketsDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), WITH_OFFERS);
        filterFavourites = new SortAndFilterMarketsDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites"), FAVOURITES);
        filterShowAll = new SortAndFilterMarketsDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.all"), ALL);

        dropdownMenu.addMenuItems(sortTitle, sortByMostOffers, sortByNameAZ, sortByNameZA, separator, filterTitle,
                filterWithOffers, filterFavourites, filterShowAll);
        return dropdownMenu;
    }

    private Button createAndGetCreateOfferButton() {
        Button createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        return createOfferButton;
    }

    private void configMarketsTableView(BisqTableView<MarketChannelItem> tableView) {
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

        searchBox.getStyleClass().add("offerbook-search-box");
        HBox subheaderContent = new HBox(30, searchBox, Spacer.fillHBox());
        subheaderContent.getStyleClass().add("offerbook-subheader-content");
        HBox.setHgrow(subheaderContent, Priority.ALWAYS);

        HBox subheader = new HBox(subheaderContent);
        subheader.getStyleClass().add("offerbook-subheader");
        subheader.setAlignment(Pos.CENTER);

        chatMessagesComponent.setMinWidth(700);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), subheader, chatMessagesComponent);
        centerVBox.setAlignment(Pos.CENTER);
    }

    private void updateChannelHeaderIcon(Node node) {
        channelHeaderIcon.setGraphic(node);
    }

    private void updateSelectedMarketFilter(BisqEasyMarketFilter bisqEasyMarketFilter) {
        if (bisqEasyMarketFilter == null) {
            return;
        }

        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof SortAndFilterMarketsDropdownMenuItem &&
                        ((SortAndFilterMarketsDropdownMenuItem<?>) menuItem).getMenuItem() instanceof BisqEasyMarketFilter)
                .map(menuItem -> (SortAndFilterMarketsDropdownMenuItem<BisqEasyMarketFilter>) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(bisqEasyMarketFilter == menuItem.getMenuItem()));

        marketsTableView.getSelectionModel().select(getModel().getSelectedMarketChannelItem().get());
    }

    private void updateMarketSortType(MarketSortType marketSortType) {
        if (marketSortType == null) {
            return;
        }

        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof SortAndFilterMarketsDropdownMenuItem &&
                        ((SortAndFilterMarketsDropdownMenuItem<?>) menuItem).getMenuItem() instanceof MarketSortType)
                .map(menuItem -> (SortAndFilterMarketsDropdownMenuItem<MarketSortType>) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(marketSortType == menuItem.getMenuItem()));
    }

    private void updateAppliedFiltersSectionStyles(boolean shouldShowAppliedFilters) {
        appliedFiltersSection.getStyleClass().clear();
        appliedFiltersSection.getStyleClass().add(shouldShowAppliedFilters
                ? "market-selection-show-applied-filters"
                : "market-selection-no-filters");
    }

    private void addCollapsedOfferList() {
        offerListGreenIcon = ImageUtil.getImageViewById("list-view-green");
        offerListGreyIcon = ImageUtil.getImageViewById("list-view-grey");
        offerListWhiteIcon = ImageUtil.getImageViewById("list-view-white");

        collapsedOfferListTitle = new Label("", offerListGreyIcon);
        collapsedOfferListTitle.setCursor(Cursor.HAND);
        collapsedOfferListTitle.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.offerList.CollapsedList.Tooltip")));
        HBox header = new HBox(collapsedOfferListTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(4, 0, 0, 0));

        HBox subheader = new HBox();
        subheader.setAlignment(Pos.CENTER_LEFT);
        subheader.getStyleClass().add("offer-list-subheader");

        collapsedOfferList = new VBox(header, Layout.hLine(), subheader, Spacer.fillVBox());
        collapsedOfferList.setMaxWidth(COLLAPSED_LIST_WIDTH);
        collapsedOfferList.setPrefWidth(COLLAPSED_LIST_WIDTH);
        collapsedOfferList.setMinWidth(COLLAPSED_LIST_WIDTH);
        collapsedOfferList.setFillWidth(true);
        collapsedOfferList.getStyleClass().add("collapsed-offer-list-container");
        HBox.setMargin(collapsedOfferList, new Insets(0, 0, 0, -9));
    }

    private void addOfferList() {
        offerListTitle = new Label(Res.get("bisqEasy.offerbook.offerList"), offerListGreenIcon);
        offerListTitle.setCursor(Cursor.HAND);
        offerListTitle.setTooltip(new BisqTooltip(Res.get("bisqEasy.offerbook.offerList.ExpandedList.Tooltip")));
        HBox header = new HBox(offerListTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 0, 15));
        header.getStyleClass().add("chat-header-title");

        DropdownMenu filterOfferListByDirection = createAndGetOfferListByDirectionFilter();

        HBox subheader = new HBox();
        subheader.setAlignment(Pos.CENTER_LEFT);
        subheader.getStyleClass().add("offer-list-subheader");
        subheader.getChildren().add(filterOfferListByDirection);

        offerListTableView = new BisqTableView<>(getModel().getSortedOfferMessageItems());
        offerListTableView.getStyleClass().add("offers-list");
        offerListTableView.allowVerticalScrollbar();
        offerListTableView.hideHorizontalScrollbar();
        offerListTableView.setFixedCellSize(getController().getMarketSelectionListCellHeight());
        offerListTableView.setPlaceholder(new Label());
        configOffersTableView(offerListTableView);
        VBox.setVgrow(offerListTableView, Priority.ALWAYS);

        offerList = new VBox(header, Layout.hLine(), subheader, offerListTableView);
        offerList.setMaxWidth(EXPANDED_OFFER_LIST_WIDTH);
        offerList.setPrefWidth(EXPANDED_OFFER_LIST_WIDTH);
        offerList.setMinWidth(EXPANDED_OFFER_LIST_WIDTH);
        offerList.setFillWidth(true);
        offerList.getStyleClass().add("chat-container");
    }

    private DropdownMenu createAndGetOfferListByDirectionFilter() {
        DropdownMenu dropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        dropdownMenu.getStyleClass().add("dropdown-offer-list-direction-filter-menu");
        offerListByDirectionFilter = new Label();
        dropdownMenu.setLabel(offerListByDirectionFilter);
        buyFromOffers = new DropdownMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.buyFrom"));
        sellToOffers = new DropdownMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.sellTo"));
        dropdownMenu.addMenuItems(buyFromOffers, sellToOffers);
        return dropdownMenu;
    }

    private void updateOfferListByDirectionFilter() {
        offerListByDirectionFilter.getStyleClass().clear();
        if (getModel().getShowBuyOffers().get()) {
            offerListByDirectionFilter.setText(sellToOffers.getLabelText());
            offerListByDirectionFilter.getStyleClass().add(SELL_TO_MENU_ITEM_STYLE_CLASS);
        } else {
            offerListByDirectionFilter.setText(buyFromOffers.getLabelText());
            offerListByDirectionFilter.getStyleClass().add(BUY_FROM_MENU_ITEM_STYLE_CLASS);
        }
    }

    private void configOffersTableView(BisqTableView<OfferMessageItem> tableView) {
        Comparator<OfferMessageItem> userProfileComparator = Comparator
                .comparingLong(OfferMessageItem::getTotalScore).reversed()
                .thenComparing(OfferMessageItem::getUserNickname);

        BisqTableColumn<OfferMessageItem> userProfileTableColumn = new BisqTableColumn.Builder<OfferMessageItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.peerProfile"))
                .left()
                .fixWidth(170)
                .setCellFactory(BisqEasyOfferbookUtil.getOfferMessageUserProfileCellFactory())
                .comparator(userProfileComparator)
                .isSortable(true)
                .build();

        BisqTableColumn<OfferMessageItem> priceTableColumn = new BisqTableColumn.Builder<OfferMessageItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.price"))
                .right()
                .fixWidth(75)
                .setCellFactory(BisqEasyOfferbookUtil.getOfferMessagePriceCellFactory())
                .comparator(Comparator.comparing(OfferMessageItem::getPriceSpecAsPercent))
                .isSortable(true)
                .build();

        BisqTableColumn<OfferMessageItem> spacerColumn = new BisqTableColumn.Builder<OfferMessageItem>()
                .fixWidth(20)
                .build();

        BisqTableColumn<OfferMessageItem> fiatAmountTableColumn = new BisqTableColumn.Builder<OfferMessageItem>()
                .titleProperty(getModel().getFiatAmountTitle())
                .right()
                .setCellFactory(BisqEasyOfferbookUtil.getOfferMessageFiatAmountCellFactory())
                .comparator(Comparator.comparing(OfferMessageItem::getMinAmount))
                .isSortable(true)
                .build();

        tableView.getColumns().add(tableView.getSelectionMarkerColumn());
        tableView.getColumns().add(userProfileTableColumn);
        tableView.getColumns().add(priceTableColumn);
        tableView.getColumns().add(spacerColumn);
        tableView.getColumns().add(fiatAmountTableColumn);

        tableView.getSortOrder().add(userProfileTableColumn);
    }

    @Getter
    private static final class SortAndFilterMarketsDropdownMenuItem<T> extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final T menuItem;

        SortAndFilterMarketsDropdownMenuItem(String defaultIconId, String activeIconId, String text, T menuItem) {
            super(defaultIconId, activeIconId, text);

            this.menuItem = menuItem;
            getStyleClass().add("dropdown-menu-item");
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
