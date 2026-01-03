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

package bisq.desktop.main.content.mu_sig.offerbook;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.asset.CryptoAsset;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownBisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.controls.DropdownTitleMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.mu_sig.MuSigOfferListItem;
import bisq.desktop.main.content.mu_sig.MuSigOfferUtil;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class MuSigOfferbookView extends View<VBox, MuSigOfferbookModel, MuSigOfferbookController> {
    private static final double HEADER_HEIGHT = 61;
    private static final double LIST_CELL_HEIGHT = 53;
    private static final double MARKET_LIST_WIDTH = 210;
    private static final double SIDE_PADDING = 40;
    private static final double FAVOURITES_TABLE_PADDING = 21;
    private static final String ACTIVE_FILTER_CLASS = "active-filter";
    private static final Map<String, StackPane> MARKET_HEADER_ICON_CACHE = new HashMap<>();

    private final RichTableView<MuSigOfferListItem> muSigOfferListView;
    private final BisqTableView<MarketItem> marketListView, favouritesListView;
    private final HBox headerHBox;
    private final VBox offersVBox;
    private final ListChangeListener<MarketItem> favouriteItemsChangeListener;
    private final ChangeListener<Toggle> toggleChangeListener;
    private final ListChangeListener<FiatPaymentMethod> availablePaymentsChangeListener;
    private final SetChangeListener<FiatPaymentMethod> selectedPaymentsChangeListener;
    private HBox appliedFiltersSection, withOffersDisplayHint, onlyFavouritesDisplayHint;
    private VBox marketListVBox;
    private Label marketListTitleLabel, marketHeaderIcon, marketTitle, marketDescription, marketPrice,
            removeWithOffersFilter, removeFavouritesFilter;
    private Button createOfferButton;
    private SearchBox marketsSearchBox;
    private DropdownMenu sortAndFilterMarketsMenu, paymentsFilterMenu, baseCurrencySelectionMenu;
    private SelectableMenuItem<CryptoAsset> btcMarketsMenuItem, xmrMarketsMenuItem;
    private SortAndFilterDropdownMenuItem<MarketSortType> sortByMostOffers, sortByNameAZ, sortByNameZA;
    private SortAndFilterDropdownMenuItem<MuSigFilters.MarketFilter> filterShowAll, filterWithOffers, filterFavourites;
    private ToggleGroup offerFiltersToggleGroup;
    private ToggleButton allOffersToggleButton, buyToggleButton, sellToggleButton, myOffersToggleButton;
    private ImageView withOffersRemoveFilterDefaultIcon, withOffersRemoveFilterActiveIcon,
            favouritesRemoveFilterDefaultIcon, favouritesRemoveFilterActiveIcon;
    private Subscription selectedMarketItemPin, marketListViewSelectionPin, favouritesListViewNeedsHeightUpdatePin,
            favouritesListViewSelectionPin, selectedMarketFilterPin, selectedMarketSortTypePin, shouldShowAppliedFiltersPin,
            selectedOffersFilterPin, activeMarketPaymentsCountPin, selectedBaseCryptoAssetPin, selectedMuSigOfferPin;
    private Label paymentsFilterLabel;

    public MuSigOfferbookView(MuSigOfferbookModel model, MuSigOfferbookController controller) {
        super(new VBox(), model, controller);

        // Offer table
        muSigOfferListView = new RichTableView<>(model.getSortedMuSigOfferListItems());
        muSigOfferListView.getSubheaderBox().setVisible(false);
        muSigOfferListView.getSubheaderBox().setManaged(false);
        muSigOfferListView.getStyleClass().add("muSig-offerbook-table");
        configMuSigOfferListView();

        // Markets column
        marketListView = new BisqTableView<>(model.getSortedMarketItems(), false);
        marketListView.getStyleClass().addAll("market-selection-list", "markets-list");
        marketListView.allowVerticalScrollbar();
        marketListView.hideHorizontalScrollbar();
        marketListView.setFixedCellSize(LIST_CELL_HEIGHT);
        marketListView.setPlaceholder(new Label());
        configMarketListView(marketListView);
        favouritesListView = new BisqTableView<>(model.getSortedFavouriteMarketItems(), false);
        favouritesListView.getStyleClass().addAll("market-selection-list", "favourites-list");
        favouritesListView.hideVerticalScrollbar();
        favouritesListView.hideHorizontalScrollbar();
        favouritesListView.setFixedCellSize(LIST_CELL_HEIGHT);
        configMarketListView(favouritesListView);
        setupMarketsColumn();

        headerHBox = new HBox(10);
        setupHeader();
        offersVBox = new VBox();
        setupOffersVBox();

        HBox marketsAndOfferTableHBox = new HBox(12, marketListVBox, offersVBox);
        VBox.setVgrow(marketsAndOfferTableHBox, Priority.ALWAYS);

        root.getChildren().add(marketsAndOfferTableHBox);
        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));

        favouriteItemsChangeListener = change -> selectedMarketItemChanged(model.getSelectedMarketItem().get());
        toggleChangeListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                updateSelectedOffersFilter(model.getSelectedMuSigOffersFilter().get());
            }
        };
        availablePaymentsChangeListener = change -> updatePaymentsFilterMenu();
        selectedPaymentsChangeListener = change -> updatePaymentsSelection();
    }

    @Override
    protected void onViewAttached() {
        muSigOfferListView.initialize();
        muSigOfferListView.resetSearch();
        muSigOfferListView.sort();

        favouritesListView.initialize();
        marketListView.initialize();

        updateAppliedFiltersSectionStyles(false);

        marketsSearchBox.textProperty().bindBidirectional(model.getMarketsSearchBoxText());
        marketTitle.textProperty().bind(model.getMarketTitle());
        marketDescription.textProperty().bind(model.getMarketDescription());
        marketPrice.textProperty().bind(model.getMarketPrice());
        favouritesListView.visibleProperty().bind(model.getShouldShowFavouritesListView());
        favouritesListView.managedProperty().bind(model.getShouldShowFavouritesListView());
        withOffersDisplayHint.visibleProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MuSigFilters.MarketFilter.WITH_OFFERS));
        withOffersDisplayHint.managedProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MuSigFilters.MarketFilter.WITH_OFFERS));
        onlyFavouritesDisplayHint.visibleProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MuSigFilters.MarketFilter.FAVOURITES));
        onlyFavouritesDisplayHint.managedProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MuSigFilters.MarketFilter.FAVOURITES));
        paymentsFilterLabel.textProperty().bind(model.getPaymentFilterTitle());
        marketListTitleLabel.textProperty().bind(model.getMarketListTitle());

        selectedMarketItemPin = EasyBind.subscribe(model.getSelectedMarketItem(), this::selectedMarketItemChanged);
        selectedBaseCryptoAssetPin = EasyBind.subscribe(model.getSelectedBaseCryptoAsset(), this::selectedBaseCryptoAssetChanged);
        marketListViewSelectionPin = EasyBind.subscribe(marketListView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                controller.onSelectMarketItem(item);
            }
        });
        favouritesListViewSelectionPin = EasyBind.subscribe(favouritesListView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                controller.onSelectMarketItem(item);
            }
        });
        model.getFavouriteMarketItems().addListener(favouriteItemsChangeListener);

        favouritesListViewNeedsHeightUpdatePin = EasyBind.subscribe(model.getFavouritesListViewNeedsHeightUpdate(), needsUpdate -> {
            if (needsUpdate) {
                double tableViewHeight = (model.getFavouriteMarketItems().size() * LIST_CELL_HEIGHT) + FAVOURITES_TABLE_PADDING;
                updateFavouritesTableViewHeight(tableViewHeight);
            }
        });

        offerFiltersToggleGroup.selectedToggleProperty().addListener(toggleChangeListener);

        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), this::updateSelectedMarketFilter);
        selectedMarketSortTypePin = EasyBind.subscribe(model.getSelectedMarketSortType(), this::updateMarketSortType);
        shouldShowAppliedFiltersPin = EasyBind.subscribe(model.getShouldShowAppliedFilters(),
                this::updateAppliedFiltersSectionStyles);
        selectedOffersFilterPin = EasyBind.subscribe(model.getSelectedMuSigOffersFilter(), this::updateSelectedOffersFilter);

        activeMarketPaymentsCountPin = EasyBind.subscribe(model.getActiveMarketPaymentsCount(), count -> {
            boolean hasActiveFilters = count.intValue() != 0;
            if (hasActiveFilters && !paymentsFilterLabel.getStyleClass().contains(ACTIVE_FILTER_CLASS)) {
                paymentsFilterLabel.getStyleClass().add(ACTIVE_FILTER_CLASS);
            } else if (!hasActiveFilters) {
                paymentsFilterLabel.getStyleClass().remove(ACTIVE_FILTER_CLASS);
            }
        });

        selectedMuSigOfferPin = EasyBind.subscribe(model.getSelectedMuSigOfferListItem(), this::trySelectingMuSigOfferListItem);

        sortByMostOffers.setOnAction(e -> controller.onSortMarkets(MarketSortType.NUM_OFFERS));
        sortByNameAZ.setOnAction(e -> controller.onSortMarkets(MarketSortType.ASC));
        sortByNameZA.setOnAction(e -> controller.onSortMarkets(MarketSortType.DESC));

        filterWithOffers.setOnAction(e -> model.getSelectedMarketsFilter().set(MuSigFilters.MarketFilter.WITH_OFFERS));
        filterShowAll.setOnAction(e -> model.getSelectedMarketsFilter().set(MuSigFilters.MarketFilter.ALL));
        filterFavourites.setOnAction(e -> model.getSelectedMarketsFilter().set(MuSigFilters.MarketFilter.FAVOURITES));

        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        removeWithOffersFilter.setOnMouseClicked(e -> model.getSelectedMarketsFilter().set(MuSigFilters.MarketFilter.ALL));
        withOffersDisplayHint.setOnMouseEntered(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterActiveIcon));
        withOffersDisplayHint.setOnMouseExited(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterDefaultIcon));

        removeFavouritesFilter.setOnMouseClicked(e -> model.getSelectedMarketsFilter().set(MuSigFilters.MarketFilter.ALL));
        onlyFavouritesDisplayHint.setOnMouseEntered(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterActiveIcon));
        onlyFavouritesDisplayHint.setOnMouseExited(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterDefaultIcon));

        allOffersToggleButton.setOnAction(e -> model.getSelectedMuSigOffersFilter().set(MuSigFilters.MuSigOffersFilter.ALL));
        buyToggleButton.setOnAction(e -> model.getSelectedMuSigOffersFilter().set(MuSigFilters.MuSigOffersFilter.SELL));
        sellToggleButton.setOnAction(e -> model.getSelectedMuSigOffersFilter().set(MuSigFilters.MuSigOffersFilter.BUY));
        myOffersToggleButton.setOnAction(e -> model.getSelectedMuSigOffersFilter().set(MuSigFilters.MuSigOffersFilter.MINE));

        btcMarketsMenuItem.setOnAction(e -> controller.updateSelectedBaseCryptoAsset(btcMarketsMenuItem.getSelectableItem().orElseThrow()));
        xmrMarketsMenuItem.setOnAction(e -> controller.updateSelectedBaseCryptoAsset(xmrMarketsMenuItem.getSelectableItem().orElseThrow()));

        model.getAvailablePaymentMethods().addListener(availablePaymentsChangeListener);
        updatePaymentsFilterMenu();
        model.getSelectedPaymentMethods().addListener(selectedPaymentsChangeListener);
        updatePaymentsSelection();
    }

    @Override
    protected void onViewDetached() {
        muSigOfferListView.dispose();

        favouritesListView.dispose();
        marketListView.dispose();

        marketsSearchBox.textProperty().unbindBidirectional(model.getMarketsSearchBoxText());
        marketTitle.textProperty().unbind();
        marketDescription.textProperty().unbind();
        marketPrice.textProperty().unbind();
        favouritesListView.visibleProperty().unbind();
        favouritesListView.managedProperty().unbind();
        withOffersDisplayHint.visibleProperty().unbind();
        withOffersDisplayHint.managedProperty().unbind();
        onlyFavouritesDisplayHint.visibleProperty().unbind();
        onlyFavouritesDisplayHint.managedProperty().unbind();
        paymentsFilterLabel.textProperty().unbind();
        marketListTitleLabel.textProperty().unbind();

        selectedMarketItemPin.unsubscribe();
        marketListViewSelectionPin.unsubscribe();
        favouritesListViewSelectionPin.unsubscribe();
        favouritesListViewNeedsHeightUpdatePin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();
        shouldShowAppliedFiltersPin.unsubscribe();
        selectedOffersFilterPin.unsubscribe();
        activeMarketPaymentsCountPin.unsubscribe();
        selectedBaseCryptoAssetPin.unsubscribe();
        selectedMuSigOfferPin.unsubscribe();

        sortByMostOffers.setOnAction(null);
        sortByNameAZ.setOnAction(null);
        sortByNameZA.setOnAction(null);
        filterWithOffers.setOnAction(null);
        filterShowAll.setOnAction(null);
        filterFavourites.setOnAction(null);
        createOfferButton.setOnAction(null);
        allOffersToggleButton.setOnAction(null);
        buyToggleButton.setOnAction(null);
        sellToggleButton.setOnAction(null);
        myOffersToggleButton.setOnAction(null);
        btcMarketsMenuItem.setOnAction(null);
        xmrMarketsMenuItem.setOnAction(null);

        removeWithOffersFilter.setOnMouseClicked(null);
        withOffersDisplayHint.setOnMouseEntered(null);
        withOffersDisplayHint.setOnMouseExited(null);

        removeFavouritesFilter.setOnMouseClicked(null);
        onlyFavouritesDisplayHint.setOnMouseEntered(null);
        onlyFavouritesDisplayHint.setOnMouseExited(null);

        model.getFavouriteMarketItems().removeListener(favouriteItemsChangeListener);
        offerFiltersToggleGroup.selectedToggleProperty().removeListener(toggleChangeListener);

        model.getAvailablePaymentMethods().removeListener(availablePaymentsChangeListener);
        model.getSelectedPaymentMethods().removeListener(selectedPaymentsChangeListener);
        cleanUpPaymentsFilterMenu();
    }

    private void configMuSigOfferListView() {
        muSigOfferListView.getColumns().add(muSigOfferListView.getTableView().getSelectionMarkerColumn());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.peer"))
                .left()
                .comparator(Comparator.comparingLong(MuSigOfferListItem::getTotalScore).reversed())
                .setCellFactory(MuSigOfferUtil.getUserProfileCellFactory())
                .minWidth(100)
                .build());

        BisqTableColumn<MuSigOfferListItem> priceColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getPriceTitle())
                .left()
                .comparator(Comparator.comparing(MuSigOfferListItem::getPrice))
                .setCellFactory(MuSigOfferUtil.getPriceCellFactory())
                .minWidth(200)
                .build();
        muSigOfferListView.getColumns().add(priceColumn);
        muSigOfferListView.getSortOrder().add(priceColumn);

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getBaseCodeTitle())
                .minWidth(120)
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountAsString))
                .setCellFactory(MuSigOfferUtil.getBaseAmountCellFactory(false))
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getQuoteCodeTitle())
                .minWidth(160)
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountAsString))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountAsString)
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .left()
                .title(Res.get("muSig.offerbook.table.header.paymentMethod"))
                .setCellFactory(MuSigOfferUtil.getPaymentCellFactory())
                .minWidth(140)
                .comparator(Comparator.comparing(MuSigOfferListItem::getPaymentMethodsAsString))
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .setCellFactory(getActionButtonsCellFactory())
                .minWidth(150)
                .build());
    }

    private void setupMarketsColumn() {
        baseCurrencySelectionMenu = createAndGetBaseCurrencySelectionDropdownMenu();
        HBox header = new HBox(baseCurrencySelectionMenu);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER);
        header.getStyleClass().add("chat-header-title");
        HBox.setHgrow(baseCurrencySelectionMenu, Priority.ALWAYS);

        marketsSearchBox = new SearchBox();
        marketsSearchBox.getStyleClass().add("offerbook-search-box");
        sortAndFilterMarketsMenu = createAndGetSortAndFilterMarketsMenu();
        HBox subheader = new HBox(marketsSearchBox, Spacer.fillHBox(), sortAndFilterMarketsMenu);
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

        marketListVBox = new VBox(header, Layout.hLine(), subheader, appliedFiltersSection, favouritesListView, marketListView);
        VBox.setVgrow(marketListView, Priority.ALWAYS);
        VBox.setVgrow(marketListVBox, Priority.ALWAYS);
        marketListVBox.setMaxWidth(MARKET_LIST_WIDTH);
        marketListVBox.setPrefWidth(MARKET_LIST_WIDTH);
        marketListVBox.setMinWidth(MARKET_LIST_WIDTH);
        marketListVBox.setFillWidth(true);
        marketListVBox.getStyleClass().add("chat-container");
    }

    private void configMarketListView(BisqTableView<MarketItem> tableView) {
        BisqTableColumn<MarketItem> marketLogoTableColumn = new BisqTableColumn.Builder<MarketItem>()
                .fixWidth(55)
                .setCellFactory(getMarketLogoCellFactory())
                .isSortable(false)
                .build();

        BisqTableColumn<MarketItem> marketLabelTableColumn = new BisqTableColumn.Builder<MarketItem>()
                .minWidth(100)
                .left()
                .setCellFactory(getMarketLabelCellFactory(tableView.equals(favouritesListView)))
                .build();

        tableView.getColumns().add(tableView.getSelectionMarkerColumn());
        tableView.getColumns().add(marketLogoTableColumn);
        tableView.getColumns().add(marketLabelTableColumn);
    }

    private static Callback<TableColumn<MarketItem, MarketItem>,
            TableCell<MarketItem, MarketItem>> getMarketLogoCellFactory() {
        return column -> new TableCell<>() {
            private final Badge numMessagesBadge = new Badge(Pos.CENTER);
            private Subscription selectedPin;

            {
                setCursor(Cursor.HAND);
                numMessagesBadge.getStyleClass().add("market-badge");
                numMessagesBadge.getLabel().setStyle("-fx-text-fill: -fx-dark-text-color !important; -fx-font-family: \"IBM Plex Sans SemiBold\";");
            }

            @Override
            protected void updateItem(MarketItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    numMessagesBadge.textProperty().bind(item.getNumMarketNotifications());

                    Node marketLogo = MarketImageComposition.createMarketLogo(item.getMarket().getQuoteCurrencyCode());
                    marketLogo.setCache(true);
                    marketLogo.setCacheHint(CacheHint.SPEED);
                    marketLogo.setEffect(MarketItem.DIMMED);

                    TableRow<MarketItem> tableRow = getTableRow();
                    if (tableRow != null) {
                        selectedPin = EasyBind.subscribe(tableRow.selectedProperty(), isSelectedMarket ->
                                marketLogo.setEffect(isSelectedMarket ? MarketItem.SELECTED : MarketItem.DIMMED));
                    }

                    StackPane pane = new StackPane(marketLogo, numMessagesBadge);
                    StackPane.setMargin(numMessagesBadge, new Insets(33, 0, 0, 35));
                    setGraphic(pane);
                } else {
                    numMessagesBadge.textProperty().unbind();
                    numMessagesBadge.setText("");
                    if (selectedPin != null) {
                        selectedPin.unsubscribe();
                        selectedPin = null;
                    }
                    setGraphic(null);
                }
            }
        };
    }

    private static Callback<TableColumn<MarketItem, MarketItem>,
            TableCell<MarketItem, MarketItem>> getMarketLabelCellFactory(boolean isFavouritesTableView) {
        return column -> new TableCell<>() {
            private final Label marketName = new Label();
            private final Label marketCode = new Label();
            private final Label numOffers = new Label();
            private final Label favouritesLabel = new Label();
            private final HBox hBox = new HBox(5, marketCode, numOffers);
            private final VBox vBox = new VBox(0, marketName, hBox);
            private final HBox container = new HBox(0, vBox, Spacer.fillHBox(), favouritesLabel);
            private final Tooltip marketDetailsTooltip = new BisqTooltip();
            private final Tooltip favouritesTooltip = new BisqTooltip();

            private static final Insets COMPACT_PADDING = new Insets(0, -10, 0, 0);

            {
                hBox.setPadding(COMPACT_PADDING);
                setCursor(Cursor.HAND);
                marketName.getStyleClass().add("market-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
                vBox.setAlignment(Pos.CENTER_LEFT);
                Tooltip.install(vBox, marketDetailsTooltip);

                favouritesTooltip.textProperty().set(isFavouritesTableView
                        ? Res.get("bisqEasy.offerbook.marketListCell.favourites.tooltip.removeFromFavourites")
                        : Res.get("bisqEasy.offerbook.marketListCell.favourites.tooltip.addToFavourites"));
                ImageView star = ImageUtil.getImageViewById(isFavouritesTableView
                        ? "star-yellow"
                        : "star-grey-hollow");
                favouritesLabel.setGraphic(star);
                favouritesLabel.getStyleClass().add("favourite-label");
                Tooltip.install(favouritesLabel, favouritesTooltip);

                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(MarketItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    numOffers.textProperty().bind(Bindings.createStringBinding(
                            () -> getFormattedOfferNumber(item.getNumOffers().get()),
                            item.getNumOffers()
                    ));
                    String quoteCurrencyDisplayName = StringUtils.capitalize(item.getMarket().getQuoteCurrencyDisplayName());
                    marketDetailsTooltip.setText(getFormattedTooltip(item.getNumOffers().get(), quoteCurrencyDisplayName));
                    marketName.setText(quoteCurrencyDisplayName);
                    marketCode.setText(item.getMarket().getQuoteCurrencyCode());
                    favouritesLabel.setOnMouseClicked(e -> item.toggleFavourite());
                    setGraphic(container);
                } else {
                    favouritesLabel.setOnMouseClicked(null);
                    numOffers.textProperty().unbind();
                    setGraphic(null);
                }
            }
        };
    }

    private static String getFormattedOfferNumber(long numOffers) {
        if (numOffers == 0) {
            return "";
        }
        return String.format("(%s)",
                numOffers > 1
                        ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.many", numOffers)
                        : Res.get("bisqEasy.offerbook.marketListCell.numOffers.one", numOffers)
        );
    }

    private static String getFormattedTooltip(long numOffers, String quoteCurrencyName) {
        if (numOffers == 0) {
            return Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.none", quoteCurrencyName);
        }
        return numOffers > 1
                ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.many", numOffers, quoteCurrencyName)
                : Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.one", numOffers, quoteCurrencyName);
    }

    private Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>, TableCell<MuSigOfferListItem, MuSigOfferListItem>> getActionButtonsCellFactory() {
        return column -> new TableCell<>() {
            private static final double PREF_WIDTH = 120;
            private static final double PREF_HEIGHT = 26;

            private final Button takeOfferButton = new Button();
            private final HBox myOfferMainBox = new HBox();
            private final HBox myOfferLabelBox = new HBox();
            private final Label myOfferLabel = new Label(Res.get("muSig.offerbook.table.cell.myOffer"));
            private final HBox myOfferActionsMenuBox = new HBox(5);
            private final BisqMenuItem removeOfferMenuItem = new BisqMenuItem("delete-t-grey", "delete-t-red");
            private final BisqMenuItem copyOfferMenuItem = new BisqMenuItem("copy-grey", "copy-white");
            private final BisqMenuItem editOfferMenuItem = new BisqMenuItem("edit-grey", "edit-white");
            private final ChangeListener<Boolean> selectedListener = (observable, oldValue, newValue) -> {
                boolean shouldShowActionsMenu = newValue || getTableRow().isHover();
                updateVisibilities(shouldShowActionsMenu);
            };

            {
                takeOfferButton.setMinWidth(PREF_WIDTH);
                takeOfferButton.setPrefWidth(PREF_WIDTH);
                takeOfferButton.setMaxWidth(PREF_WIDTH);
                takeOfferButton.getStyleClass().add("mu-sig-offerbook-offerlist-take-offer-button");
                takeOfferButton.setMinHeight(PREF_HEIGHT);
                takeOfferButton.setPrefHeight(PREF_HEIGHT);
                takeOfferButton.setMaxHeight(PREF_HEIGHT);

                myOfferMainBox.setMinWidth(PREF_WIDTH);
                myOfferMainBox.setPrefWidth(PREF_WIDTH);
                myOfferMainBox.setMaxWidth(PREF_WIDTH);
                myOfferMainBox.setMinHeight(PREF_HEIGHT);
                myOfferMainBox.setPrefHeight(PREF_HEIGHT);
                myOfferMainBox.setMaxHeight(PREF_HEIGHT);
                myOfferMainBox.getChildren().addAll(myOfferLabelBox, myOfferActionsMenuBox);

                myOfferLabelBox.setMinWidth(PREF_WIDTH);
                myOfferLabelBox.setPrefWidth(PREF_WIDTH);
                myOfferLabelBox.setMaxWidth(PREF_WIDTH);
                myOfferLabelBox.setMinHeight(PREF_HEIGHT);
                myOfferLabelBox.setPrefHeight(PREF_HEIGHT);
                myOfferLabelBox.setMaxHeight(PREF_HEIGHT);
                myOfferLabelBox.getChildren().add(myOfferLabel);
                myOfferLabelBox.getStyleClass().add("mu-sig-offerbook-offerlist-myOffer-label-box");
                myOfferLabelBox.setAlignment(Pos.CENTER);

                myOfferActionsMenuBox.setMinWidth(PREF_WIDTH);
                myOfferActionsMenuBox.setPrefWidth(PREF_WIDTH);
                myOfferActionsMenuBox.setMaxWidth(PREF_WIDTH);
                myOfferActionsMenuBox.setMinHeight(PREF_HEIGHT);
                myOfferActionsMenuBox.setPrefHeight(PREF_HEIGHT);
                myOfferActionsMenuBox.setMaxHeight(PREF_HEIGHT);
                myOfferActionsMenuBox.getChildren().addAll(editOfferMenuItem, copyOfferMenuItem, removeOfferMenuItem);
                myOfferActionsMenuBox.setAlignment(Pos.CENTER);

                removeOfferMenuItem.useIconOnly();
                removeOfferMenuItem.setTooltip(Res.get("offer.delete"));

                copyOfferMenuItem.useIconOnly();
                copyOfferMenuItem.setTooltip(Res.get("offer.copy"));

                editOfferMenuItem.useIconOnly();
                editOfferMenuItem.setTooltip(Res.get("offer.edit"));
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                resetRowEventHandlersAndListeners();
                resetVisibilities();
                resetStyles();

                if (item != null && !empty) {
                    if (item.isMyOffer()) {
                        setUpRowEventHandlersAndListeners();
                        if (item.getOffer().getDirection().mirror().isBuy()) {
                            myOfferLabelBox.getStyleClass().add("my-offer-to-buy");
                            myOfferLabel.setStyle("-fx-text-fill: -bisq2-green-dim-10;");
                        } else {
                            myOfferLabelBox.getStyleClass().add("my-offer-to-sell");
                            myOfferLabel.setStyle("-fx-text-fill: -bisq2-red-lit-10;");
                        }
                        setGraphic(myOfferMainBox);
                        removeOfferMenuItem.setOnAction(e -> controller.onRemoveOffer(item.getOffer()));
                    } else {
                        takeOfferButton.setText(item.getTakeOfferButtonText());
                        boolean canTakeOffer = item.isCanTakeOffer();
                        takeOfferButton.setOpacity(canTakeOffer ? 1 : 0.2);
                        if (item.getOffer().getDirection().mirror().isBuy()) {
                            takeOfferButton.getStyleClass().add("buy-button");
                        } else {
                            takeOfferButton.getStyleClass().add("sell-button");
                        }
                        if (canTakeOffer) {
                            takeOfferButton.setOnAction(e -> controller.onTakeOffer(item.getOffer()));
                        } else {
                            takeOfferButton.setOnAction(e -> controller.onHandleCannotTakeOfferCase(item.getCannotTakeOfferReason().get()));
                        }
                        setGraphic(takeOfferButton);
                    }
                } else {
                    resetStyles();
                    resetRowEventHandlersAndListeners();
                    resetVisibilities();
                    takeOfferButton.setOnAction(null);
                    removeOfferMenuItem.setOnAction(null);
                    setGraphic(null);
                }
            }

            private void resetStyles() {
                takeOfferButton.getStyleClass().remove("buy-button");
                takeOfferButton.getStyleClass().remove("sell-button");
                takeOfferButton.getStyleClass().remove("white-transparent-outlined-button");
                myOfferLabelBox.getStyleClass().remove("my-offer-to-buy");
                myOfferLabelBox.getStyleClass().remove("my-offer-to-sell");
                myOfferLabel.getStyleClass().clear();
            }

            private void setUpRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(e -> {
                        boolean shouldShowActionsMenu = row.isSelected() || row.isHover();
                        updateVisibilities(shouldShowActionsMenu);
                    });
                    row.setOnMouseExited(e -> {
                        boolean shouldShowActionsMenu = row.isSelected();
                        updateVisibilities(shouldShowActionsMenu);
                    });
                    row.selectedProperty().addListener(selectedListener);
                }
            }

            private void resetRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(null);
                    row.setOnMouseExited(null);
                    row.selectedProperty().removeListener(selectedListener);
                }
            }

            private void resetVisibilities() {
                updateVisibilities(false);
            }

            private void updateVisibilities(boolean shouldShowActionsMenu) {
                myOfferLabelBox.setVisible(!shouldShowActionsMenu);
                myOfferLabelBox.setManaged(!shouldShowActionsMenu);
                myOfferActionsMenuBox.setVisible(shouldShowActionsMenu);
                myOfferActionsMenuBox.setManaged(shouldShowActionsMenu);
            }
        };
    }

    private void selectedMarketItemChanged(MarketItem selectedItem) {
        marketListView.getSelectionModel().clearSelection();
        marketListView.getSelectionModel().select(selectedItem);
        favouritesListView.getSelectionModel().clearSelection();
        favouritesListView.getSelectionModel().select(selectedItem);

        if (selectedItem != null) {
            if (marketHeaderIcon != null) {
                StackPane tradePairImage = MarketImageComposition.getMarketIcons(selectedItem.getMarket(), MARKET_HEADER_ICON_CACHE);
                marketHeaderIcon.setGraphic(tradePairImage);
            }
        }
    }

    private void selectedBaseCryptoAssetChanged(CryptoAsset selectedBaseCryptoAsset) {
        if (selectedBaseCryptoAsset != null) {
            Node baseCryptoImage = MarketImageComposition.createMarketLogo(model.getBaseCurrencyIconId().get());
            marketListTitleLabel.setGraphic(baseCryptoImage);

            //noinspection unchecked
            baseCurrencySelectionMenu.getMenuItems().stream()
                    .filter(item -> item instanceof SelectableMenuItem)
                    .map(item -> (SelectableMenuItem<CryptoAsset>) item)
                    .forEach(selectableMenuItem -> {
                        selectableMenuItem.getSelectableItem().ifPresent(cryptoAsset ->
                                selectableMenuItem.updateSelection(cryptoAsset.equals(selectedBaseCryptoAsset)));
            });
        }
    }

    private void setupHeader() {
        headerHBox.getStyleClass().add("chat-container-header");

        marketDescription = new Label();
        marketDescription.getStyleClass().addAll("chat-header-description", "offerbook-channel-market-code");
        marketPrice = new Label();
        marketPrice.getStyleClass().addAll("chat-header-description", "offerbook-channel-market-price");
        HBox marketDescriptionHbox = new HBox(5, marketDescription, marketPrice);

        marketTitle = new Label();
        marketTitle.getStyleClass().add("mu-sig-offerbook-market-title");
        marketTitle.setPadding(new Insets(3, 0, 0, 0));
        VBox titleAndDescription = new VBox(marketTitle, marketDescriptionHbox);

        marketHeaderIcon = new Label();
        HBox headerTitle = new HBox(10, marketHeaderIcon, titleAndDescription);
        headerTitle.setAlignment(Pos.CENTER_LEFT);

        createOfferButton = createAndGetCreateOfferButton();
        createOfferButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);

        HBox.setHgrow(headerTitle, Priority.ALWAYS);
        headerHBox.getChildren().setAll(headerTitle, createOfferButton);
    }

    private Button createAndGetCreateOfferButton() {
        Button createOfferButton = new Button(Res.get("offer.create"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        return createOfferButton;
    }

    private void setupOffersVBox() {
        allOffersToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerListSubheader.offersToggleGroup.allOffers"));
        allOffersToggleButton.getStyleClass().add("offerlist-toggle-button-all-offers");
        buyToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerListSubheader.offersToggleGroup.buy"));
        buyToggleButton.getStyleClass().add("offerlist-toggle-button-buy");
        sellToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerListSubheader.offersToggleGroup.sell"));
        sellToggleButton.getStyleClass().add("offerlist-toggle-button-sell");
        myOffersToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerListSubheader.offersToggleGroup.myOffers"));
        myOffersToggleButton.getStyleClass().add("offerlist-toggle-button-my-offers");

        offerFiltersToggleGroup = new ToggleGroup();
        offerFiltersToggleGroup.getToggles().addAll(allOffersToggleButton, buyToggleButton, sellToggleButton, myOffersToggleButton);
        HBox toggleButtonHBox = new HBox(3, allOffersToggleButton, buyToggleButton, sellToggleButton, myOffersToggleButton);
        toggleButtonHBox.getStyleClass().add("toggle-button-hbox");

        // Add payments filter menu to subheader
        paymentsFilterMenu = createAndGetPaymentsFilterDropdownMenu();
        HBox subheaderContent = new HBox(toggleButtonHBox, Spacer.fillHBox(), paymentsFilterMenu);
        subheaderContent.getStyleClass().add("mu-sig-offerbook-subheader-content");
        subheaderContent.setPadding(new Insets(0, 12, 0, 13));
        HBox.setHgrow(subheaderContent, Priority.ALWAYS);

        HBox subheader = new HBox(subheaderContent);
        subheader.getStyleClass().add("offerbook-subheader");
        subheader.setAlignment(Pos.CENTER);

        offersVBox.getChildren().addAll(headerHBox, Layout.hLine(), subheader, muSigOfferListView);
        offersVBox.getStyleClass().add("bisq-easy-container");
        VBox.setVgrow(muSigOfferListView, Priority.ALWAYS);
        HBox.setHgrow(offersVBox, Priority.ALWAYS);
        VBox.setVgrow(offersVBox, Priority.ALWAYS);
    }

    private void updateAppliedFiltersSectionStyles(boolean shouldShowAppliedFilters) {
        appliedFiltersSection.getStyleClass().clear();
        appliedFiltersSection.getStyleClass().add(shouldShowAppliedFilters
                ? "market-selection-show-applied-filters"
                : "market-selection-no-filters");
    }

    private void updateFavouritesTableViewHeight(double height) {
        favouritesListView.setMinHeight(height);
        favouritesListView.setPrefHeight(height);
        favouritesListView.setMaxHeight(height);
        model.getFavouritesListViewNeedsHeightUpdate().set(false);
    }

    private DropdownMenu createAndGetBaseCurrencySelectionDropdownMenu() {
        DropdownMenu menu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        menu.getStyleClass().add("base-currency-dropdown-menu");
        marketListTitleLabel = new Label();
        marketListTitleLabel.setGraphicTextGap(10);
        menu.setContent(marketListTitleLabel);

        CryptoAsset btc = CryptoAssetRepository.BITCOIN;
        Label btcMarketLabel = new Label(String.format("%s %s", btc.getCode(), btc.getName()));
        btcMarketLabel.setGraphicTextGap(10);
        btcMarketLabel.setGraphic(getMarketLogo(btc.getCode()));
        btcMarketsMenuItem = new SelectableMenuItem<>(btc, btcMarketLabel);

        CryptoAsset xmr = CryptoAssetRepository.XMR;
        Label xmrMarketLabel = new Label(String.format("%s %s", xmr.getCode(), xmr.getName()));
        xmrMarketLabel.setGraphicTextGap(10);
        xmrMarketLabel.setGraphic(getMarketLogo(xmr.getCode()));
        xmrMarketsMenuItem = new SelectableMenuItem<>(xmr, xmrMarketLabel);

        menu.addMenuItems(btcMarketsMenuItem, xmrMarketsMenuItem);
        return menu;
    }

    private Node getMarketLogo(String code) {
        Node marketLogo = MarketImageComposition.createMarketMenuLogo(code);
        marketLogo.setCache(true);
        marketLogo.setCacheHint(CacheHint.SPEED);
        return marketLogo;
    }

    private DropdownMenu createAndGetPaymentsFilterDropdownMenu() {
        DropdownMenu menu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        menu.getStyleClass().add("dropdown-offer-list-payment-filter-menu");
        paymentsFilterLabel = new Label();
        menu.setContent(paymentsFilterLabel);
        return menu;
    }

    private DropdownMenu createAndGetSortAndFilterMarketsMenu() {
        DropdownMenu dropdownMenu = new DropdownMenu("sort-grey", "sort-white", true);
        dropdownMenu.setTooltip(Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.tooltip"));
        dropdownMenu.getStyleClass().add("market-selection-dropdown-menu");

        // Sorting options
        DropdownTitleMenuItem sortTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.sortTitle"));
        sortByMostOffers = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.mostOffers"), MarketSortType.NUM_OFFERS);
        sortByNameAZ = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameAZ"), MarketSortType.ASC);
        sortByNameZA = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.nameZA"), MarketSortType.DESC);

        // Separator
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Filter options
        DropdownTitleMenuItem filterTitle = new DropdownTitleMenuItem(
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.filterTitle"));
        filterWithOffers = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), MuSigFilters.MarketFilter.WITH_OFFERS);
        filterFavourites = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites"), MuSigFilters.MarketFilter.FAVOURITES);
        filterShowAll = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.all"), MuSigFilters.MarketFilter.ALL);

        dropdownMenu.addMenuItems(sortTitle, sortByMostOffers, sortByNameAZ, sortByNameZA, separator, filterTitle,
                filterWithOffers, filterFavourites, filterShowAll);
        return dropdownMenu;
    }

    private void updateSelectedMarketFilter(MuSigFilters.MarketFilter bisqEasyMarketFilter) {
        if (bisqEasyMarketFilter == null) {
            return;
        }

        //noinspection unchecked
        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof SortAndFilterDropdownMenuItem &&
                        ((SortAndFilterDropdownMenuItem<?>) menuItem).getMenuItem() instanceof MuSigFilters.MarketFilter)
                .map(menuItem -> (SortAndFilterDropdownMenuItem<MuSigFilters.MarketFilter>) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(bisqEasyMarketFilter == menuItem.getMenuItem()));

        marketListView.getSelectionModel().select(model.getSelectedMarketItem().get());
    }

    private void updateMarketSortType(MarketSortType marketSortType) {
        if (marketSortType == null) {
            return;
        }

        //noinspection unchecked
        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof SortAndFilterDropdownMenuItem &&
                        ((SortAndFilterDropdownMenuItem<?>) menuItem).getMenuItem() instanceof MarketSortType)
                .map(menuItem -> (SortAndFilterDropdownMenuItem<MarketSortType>) menuItem)
                .forEach(menuItem -> menuItem.updateSelection(marketSortType == menuItem.getMenuItem()));
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

    private void updateSelectedOffersFilter(MuSigFilters.MuSigOffersFilter offerDirectionFilter) {
        if (offerDirectionFilter == MuSigFilters.MuSigOffersFilter.ALL) {
            allOffersToggleButton.setSelected(true);
        } else if (offerDirectionFilter == MuSigFilters.MuSigOffersFilter.BUY) {
            sellToggleButton.setSelected(true);
        } else if (offerDirectionFilter == MuSigFilters.MuSigOffersFilter.SELL) {
            buyToggleButton.setSelected(true);
        } else if (offerDirectionFilter == MuSigFilters.MuSigOffersFilter.MINE) {
            myOffersToggleButton.setSelected(true);
        }
    }

    private void updatePaymentsFilterMenu() {
        cleanUpPaymentsFilterMenu();

        model.getAvailablePaymentMethods().forEach(paymentMethod -> {
            ImageView paymentIcon = ImageUtil.getImageViewById(paymentMethod.getPaymentRailName());
            Label paymentLabel = new Label(paymentMethod.getDisplayString(), paymentIcon);
            paymentLabel.setGraphicTextGap(10);
            SelectableMenuItem<FiatPaymentMethod> paymentItem = new SelectableMenuItem<>(paymentMethod, paymentLabel);
            paymentItem.setHideOnClick(false);
            paymentItem.setOnAction(e -> controller.onTogglePaymentFilter(paymentMethod, paymentItem.isSelected()));
            paymentsFilterMenu.addMenuItems(paymentItem);
        });

        SeparatorMenuItem separator = new SeparatorMenuItem();
        DropdownBisqMenuItem clearFilters = new DropdownBisqMenuItem("delete-t-grey", "delete-t-white",
                Res.get("muSig.offerbook.offerListSubheader.paymentMethods.clearFilters"));
        clearFilters.setHideOnClick(false);
        clearFilters.setOnAction(e -> controller.onClearPaymentFilters());
        paymentsFilterMenu.addMenuItems(separator, clearFilters);
    }

    private void updatePaymentsSelection() {
        //noinspection unchecked
        paymentsFilterMenu.getMenuItems().stream()
                .filter(item -> item instanceof SelectableMenuItem)
                .map(item -> (SelectableMenuItem<FiatPaymentMethod>) item)
                .forEach(paymentMenuItem ->
                        paymentMenuItem.getSelectableItem()
                                .ifPresentOrElse(
                                        payment -> paymentMenuItem.updateSelection(model.getSelectedPaymentMethods().contains(payment)),
                                        () -> paymentMenuItem.updateSelection(false))
                );
    }

    private void cleanUpPaymentsFilterMenu() {
        //noinspection unchecked
        paymentsFilterMenu.getMenuItems().stream()
                .filter(item -> item instanceof SelectableMenuItem)
                .map(item -> (SelectableMenuItem<FiatPaymentMethod>) item)
                .forEach(SelectableMenuItem::dispose);
        paymentsFilterMenu.clearMenuItems();
    }

    private void trySelectingMuSigOfferListItem(MuSigOfferListItem muSigOfferListItem) {
        if (muSigOfferListItem != null) {
            boolean isBuyOfferWithSellFilter = muSigOfferListItem.getDirection().isBuy()
                    && model.getSelectedMuSigOffersFilter().get() == MuSigFilters.MuSigOffersFilter.SELL;
            boolean isSellOfferWithBuyFilter = muSigOfferListItem.getDirection().isSell()
                    && model.getSelectedMuSigOffersFilter().get() == MuSigFilters.MuSigOffersFilter.BUY;
            if (isBuyOfferWithSellFilter || isSellOfferWithBuyFilter) {
                model.getSelectedMuSigOffersFilter().set(MuSigFilters.MuSigOffersFilter.ALL);
            }
            model.getSelectedMarket().set(muSigOfferListItem.getMarket());
            muSigOfferListView.getTableView().getSelectionModel().select(muSigOfferListItem);
            muSigOfferListView.getTableView().scrollTo(muSigOfferListItem);
            model.getSelectedMuSigOfferListItem().set(null);
        }
    }

    @Getter
    private static final class SortAndFilterDropdownMenuItem<T> extends DropdownBisqMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final T menuItem;

        SortAndFilterDropdownMenuItem(String defaultIconId, String activeIconId, String text, T menuItem) {
            super(defaultIconId, activeIconId, text);

            this.menuItem = menuItem;
            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }
    }

    @Getter
    private static final class SelectableMenuItem<T> extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final Optional<T> selectableItem;

        private SelectableMenuItem(T selectableItem, Label displayLabel) {
            super("check-white", "check-white", displayLabel);

            this.selectableItem = Optional.ofNullable(selectableItem);
            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
        }

        public void dispose() {
            setOnAction(null);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }

        boolean isSelected() {
            return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
        }
    }
}
