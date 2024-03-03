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
    private Subscription tableViewSelectionPin, selectedModelItemPin, channelHeaderIconPin, selectedMarketFilterPin,
            selectedOfferDirectionOrOwnerFilterPin, selectedPeerReputationFilterPin, selectedMarketSortTypePin;
    private Button createOfferButton;
    private DropdownMenu sortAndFilterMarketsMenu, filterOffersByDirectionOrOwnerMenu, filterOffersByPeerReputationMenu;
    private DropdownSortByMenuItem sortByMostOffers, sortByNameAZ, sortByNameZA;
    private DropdownFilterMenuItem<MarketChannelItem> filterShowAll, filterWithOffers;
    private DropdownFilterMenuItem<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>
            allOffers, myOffers, buyOffers, sellOffers, allReputations, fiveStars, atLeastFourStars, atLeastThreeStars,
            atLeastTwoStars, atLeastOneStar;
    private DropdownTitleMenuItem atLeastTitle;
    private CheckBox hideUserMessagesCheckbox;
    private Label channelHeaderIcon, marketPrice;

    public BisqEasyOfferbookView(BisqEasyOfferbookModel model,
                                 BisqEasyOfferbookController controller,
                                 VBox chatMessagesComponent,
                                 Pane channelSidebar) {
        super(model, controller, chatMessagesComponent, channelSidebar);
    }

    @Override
    protected void configTitleHBox() {
        super.configTitleHBox();

        marketPrice = new Label();
        HBox marketDescription = new HBox(5, channelDescription, marketPrice);
        VBox titleAndDescription = new VBox(channelTitle, marketDescription);
        channelTitle.getStyleClass().add("chat-header-title");

        channelHeaderIcon = new Label();
        HBox headerTitle = new HBox(10, channelHeaderIcon, titleAndDescription);
        headerTitle.setAlignment(Pos.CENTER_LEFT);
        headerTitle.setPadding(new Insets(7, 0, 0, 0));
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

        selectedModelItemPin = EasyBind.subscribe(getModel().getSelectedMarketChannelItem(), selected -> {
            tableView.getSelectionModel().select(selected);
        });
        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectMarketChannelItem(item);
            }
        });
        channelHeaderIconPin = EasyBind.subscribe(model.getChannelIconNode(), this::updateChannelHeaderIcon);
        selectedMarketFilterPin = EasyBind.subscribe(getModel().getSelectedMarketsFilter(), this::updateSelectedMarketFilter);
        selectedOfferDirectionOrOwnerFilterPin = EasyBind.subscribe(getModel().getSelectedOfferDirectionOrOwnerFilter(), filter ->
                updateSelectedFilterInDropdownMenu(filter, filterOffersByDirectionOrOwnerMenu));
        selectedPeerReputationFilterPin = EasyBind.subscribe(getModel().getSelectedPeerReputationFilter(), filter ->
                updateSelectedFilterInDropdownMenu(filter, filterOffersByPeerReputationMenu));
        selectedMarketSortTypePin = EasyBind.subscribe(getModel().getSelectedMarketSortType(), this::updateMarketSortType);

        sortByMostOffers.setOnAction(e -> getController().onSortMarkets(MarketSortType.NUM_OFFERS));
        sortByNameAZ.setOnAction(e -> getController().onSortMarkets(MarketSortType.ASC));
        sortByNameZA.setOnAction(e -> getController().onSortMarkets(MarketSortType.DESC));

        filterWithOffers.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.WITH_OFFERS));
        filterShowAll.setOnAction(e -> getModel().getSelectedMarketsFilter().set(Filters.Markets.ALL));

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

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        channelHeaderIconPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedOfferDirectionOrOwnerFilterPin.unsubscribe();
        selectedPeerReputationFilterPin.unsubscribe();
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
        allReputations.setOnAction(null);
        fiveStars.setOnAction(null);
        atLeastFourStars.setOnAction(null);
        atLeastThreeStars.setOnAction(null);
        atLeastTwoStars.setOnAction(null);
        atLeastOneStar.setOnAction(null);
        createOfferButton.setOnAction(null);
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

        marketSelectionList = new VBox(header, Layout.hLine(), subheader, tableView);
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
        DropdownTitleMenuItem sortTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.sortTitle"));
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
        DropdownTitleMenuItem filterTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.filterTitle"));
        filterWithOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), Filters.Markets.WITH_OFFERS);
        filterShowAll = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.all"), Filters.Markets.ALL);

        dropdownMenu.addMenuItems(sortTitle, sortByMostOffers, sortByNameAZ, sortByNameZA, separator, filterTitle,
                filterWithOffers, filterShowAll);
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

        filterOffersByPeerReputationMenu = createAndGetPeerReputationFilterMenu();
        filterOffersByDirectionOrOwnerMenu = createAndGetOfferDirectionOrOwnerFilterMenu();
        
        HBox subheaderContent = new HBox(30, checkbox, filterOffersByPeerReputationMenu, filterOffersByDirectionOrOwnerMenu);
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
        DropdownMenu dropdownMenu = new DropdownMenu("arrow-down", "arrow-down", false);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.allOffers"), Filters.OfferDirectionOrOwner.ALL);
        myOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.myOffers"), Filters.OfferDirectionOrOwner.MINE);
        buyOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.buyOffers"), Filters.OfferDirectionOrOwner.BUY);
        sellOffers = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByDirectionOrOwner.sellOffers"), Filters.OfferDirectionOrOwner.SELL);

        dropdownMenu.addMenuItems(sellOffers, buyOffers, myOffers, allOffers);
        return dropdownMenu;
    }

    private DropdownMenu createAndGetPeerReputationFilterMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("arrow-down", "arrow-down", false);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.tooltip"));
        dropdownMenu.getStyleClass().add("dropdown-offers-filter-menu");

        allReputations = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.allReputations"),
                Filters.PeerReputation.ALL);
        fiveStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.fiveStars"),
                Filters.PeerReputation.FIVE_STARS);
        atLeastTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastTitle"));
        atLeastFourStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastFourStars"),
                Filters.PeerReputation.AT_LEAST_FOUR_STARS);
        atLeastThreeStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastThreeStars"),
                Filters.PeerReputation.AT_LEAST_THREE_STARS);
        atLeastTwoStars = new DropdownFilterMenuItem<>("check-grey", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.filterOffersByPeerReputation.atLeastTwoStars"),
                Filters.PeerReputation.AT_LEAST_TWO_STARS);
        atLeastOneStar = new DropdownFilterMenuItem<>("check-grey", "check-white",
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
