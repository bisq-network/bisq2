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

import bisq.common.util.StringUtils;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownBisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownTitleMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
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

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public final class MuSigOfferbookView extends View<VBox, MuSigOfferbookModel, MuSigOfferbookController> {
    private final static double HEADER_HEIGHT = 61;
    private static final double LIST_CELL_HEIGHT = 53;
    private static final double MARKET_LIST_WIDTH = 210;
    private static final double SIDE_PADDING = 40;
    private static final double FAVOURITES_TABLE_PADDING = 21;

    private final RichTableView<MuSigOfferListItem> muSigOfferListView;
    private final BisqTableView<MarketItem> marketListView, favouritesListView;
    private final HBox headerHBox;
    private final VBox offersVBox;
    private final ListChangeListener<MarketItem> favouriteItemsChangeListener;
    private final ChangeListener<Toggle> toggleChangeListener;
    private HBox appliedFiltersSection, withOffersDisplayHint, onlyFavouritesDisplayHint;
    private VBox marketListVBox;
    private Label marketListTitle, marketHeaderIcon, marketTitle, marketDescription, marketPrice,
            removeWithOffersFilter, removeFavouritesFilter;
    private Button createOfferButton;
    private SearchBox marketsSearchBox;
    private DropdownMenu sortAndFilterMarketsMenu;
    private SortAndFilterDropdownMenuItem<MarketSortType> sortByMostOffers, sortByNameAZ, sortByNameZA;
    private SortAndFilterDropdownMenuItem<MarketFilter> filterShowAll, filterWithOffers, filterFavourites;
    private ToggleGroup offerlistFiltersToggleGroup;
    private ToggleButton allOffersToggleButton, buyToggleButton, sellToggleButton;
    private ImageView withOffersRemoveFilterDefaultIcon, withOffersRemoveFilterActiveIcon,
            favouritesRemoveFilterDefaultIcon, favouritesRemoveFilterActiveIcon;
    private Subscription selectedMarketItemPin, marketListViewSelectionPin, favouritesListViewNeedsHeightUpdatePin,
            favouritesListViewSelectionPin, selectedMarketFilterPin, selectedMarketSortTypePin, shouldShowAppliedFiltersPin,
            selectedOfferlistFilterPin;

    public MuSigOfferbookView(MuSigOfferbookModel model, MuSigOfferbookController controller) {
        super(new VBox(), model, controller);

        // Offer table
        muSigOfferListView = new RichTableView<>(model.getSortedMuSigOfferListItems());
        muSigOfferListView.getFooterVBox().setVisible(false);
        muSigOfferListView.getFooterVBox().setManaged(false);
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
                updateSelectedOfferlistFilter(model.getSelectedMuSigOfferlistFilter().get());
            }
        };
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
        withOffersDisplayHint.visibleProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MarketFilter.WITH_OFFERS));
        withOffersDisplayHint.managedProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MarketFilter.WITH_OFFERS));
        onlyFavouritesDisplayHint.visibleProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MarketFilter.FAVOURITES));
        onlyFavouritesDisplayHint.managedProperty().bind(model.getSelectedMarketsFilter().isEqualTo(MarketFilter.FAVOURITES));

        selectedMarketItemPin = EasyBind.subscribe(model.getSelectedMarketItem(), this::selectedMarketItemChanged);
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

        offerlistFiltersToggleGroup.selectedToggleProperty().addListener(toggleChangeListener);

        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), this::updateSelectedMarketFilter);
        selectedMarketSortTypePin = EasyBind.subscribe(model.getSelectedMarketSortType(), this::updateMarketSortType);
        shouldShowAppliedFiltersPin = EasyBind.subscribe(model.getShouldShowAppliedFilters(),
                this::updateAppliedFiltersSectionStyles);
        selectedOfferlistFilterPin = EasyBind.subscribe(model.getSelectedMuSigOfferlistFilter(), this::updateSelectedOfferlistFilter);

        sortByMostOffers.setOnAction(e -> controller.onSortMarkets(MarketSortType.NUM_OFFERS));
        sortByNameAZ.setOnAction(e -> controller.onSortMarkets(MarketSortType.ASC));
        sortByNameZA.setOnAction(e -> controller.onSortMarkets(MarketSortType.DESC));

        filterWithOffers.setOnAction(e -> model.getSelectedMarketsFilter().set(MarketFilter.WITH_OFFERS));
        filterShowAll.setOnAction(e -> model.getSelectedMarketsFilter().set(MarketFilter.ALL));
        filterFavourites.setOnAction(e -> model.getSelectedMarketsFilter().set(MarketFilter.FAVOURITES));

        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        removeWithOffersFilter.setOnMouseClicked(e -> model.getSelectedMarketsFilter().set(MarketFilter.ALL));
        withOffersDisplayHint.setOnMouseEntered(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterActiveIcon));
        withOffersDisplayHint.setOnMouseExited(e -> removeWithOffersFilter.setGraphic(withOffersRemoveFilterDefaultIcon));

        removeFavouritesFilter.setOnMouseClicked(e -> model.getSelectedMarketsFilter().set(MarketFilter.ALL));
        onlyFavouritesDisplayHint.setOnMouseEntered(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterActiveIcon));
        onlyFavouritesDisplayHint.setOnMouseExited(e -> removeFavouritesFilter.setGraphic(favouritesRemoveFilterDefaultIcon));

        allOffersToggleButton.setOnAction(e -> model.getSelectedMuSigOfferlistFilter().set(null));
        buyToggleButton.setOnAction(e -> model.getSelectedMuSigOfferlistFilter().set(Direction.SELL));
        sellToggleButton.setOnAction(e -> model.getSelectedMuSigOfferlistFilter().set(Direction.BUY));
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

        selectedMarketItemPin.unsubscribe();
        marketListViewSelectionPin.unsubscribe();
        favouritesListViewSelectionPin.unsubscribe();
        favouritesListViewNeedsHeightUpdatePin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedMarketSortTypePin.unsubscribe();
        shouldShowAppliedFiltersPin.unsubscribe();
        selectedOfferlistFilterPin.unsubscribe();

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

        removeWithOffersFilter.setOnMouseClicked(null);
        withOffersDisplayHint.setOnMouseEntered(null);
        withOffersDisplayHint.setOnMouseExited(null);

        removeFavouritesFilter.setOnMouseClicked(null);
        onlyFavouritesDisplayHint.setOnMouseEntered(null);
        onlyFavouritesDisplayHint.setOnMouseExited(null);

        model.getFavouriteMarketItems().removeListener(favouriteItemsChangeListener);
        offerlistFiltersToggleGroup.selectedToggleProperty().removeListener(toggleChangeListener);
    }

    private void configMuSigOfferListView() {
        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.peerProfile"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getMaker))
                .valueSupplier(MuSigOfferListItem::getMaker)
                .minWidth(200)
                .build());

        BisqTableColumn<MuSigOfferListItem> priceColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getPriceTitle())
                .left()
                .comparator(Comparator.comparing(MuSigOfferListItem::getPrice))
                .valueSupplier(MuSigOfferListItem::getPrice)
                .tooltipSupplier(MuSigOfferListItem::getPriceTooltip)
                .build();
        muSigOfferListView.getColumns().add(priceColumn);
        muSigOfferListView.getSortOrder().add(priceColumn);

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getBaseCodeTitle())
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountAsString))
                .valueSupplier(MuSigOfferListItem::getBaseAmountAsString)
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .titleProperty(model.getQuoteCodeTitle())
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountAsString))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountAsString)
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.paymentMethod"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getPaymentMethod))
                .valueSupplier(MuSigOfferListItem::getPaymentMethod)
                .tooltipSupplier(MuSigOfferListItem::getPaymentMethodTooltip)
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.offerbook.table.header.deposit"))
                .comparator(Comparator.comparing(MuSigOfferListItem::getDeposit))
                .valueSupplier(MuSigOfferListItem::getDeposit)
                .build());

        muSigOfferListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .setCellFactory(getActionButtonCellFactory())
                .minWidth(150)
                .build());
    }

    private void setupMarketsColumn() {
        marketListTitle = new Label(Res.get("bisqEasy.offerbook.markets"));
        marketListTitle.setGraphicTextGap(10);
        HBox.setHgrow(marketListTitle, Priority.ALWAYS);

        HBox header = new HBox(marketListTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 12, 0, 13));
        header.getStyleClass().add("chat-header-title");

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
                    setGraphic(null);
                    numOffers.textProperty().unbind();
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

    private Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>, TableCell<MuSigOfferListItem, MuSigOfferListItem>> getActionButtonCellFactory() {
        return column -> new TableCell<>() {
            private final Button takeOfferButton = new Button();

            {
                takeOfferButton.setMinWidth(110);
                takeOfferButton.setMaxWidth(takeOfferButton.getMinWidth());
                takeOfferButton.getStyleClass().add("button-min-horizontal-padding");
                takeOfferButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    if (item.isMyOffer()) {
                        takeOfferButton.setText(Res.get("muSig.offerbook.table.cell.intent.remove").toUpperCase(Locale.ROOT));
                        resetStyles();
                        // FIXME Label text always stays white independent of style class or even if setting style here directly.
                        //  If using grey-transparent-outlined-button we have a white label. Quick fix is to use opacity with a while style...
                        takeOfferButton.getStyleClass().add("white-transparent-outlined-button");
                        takeOfferButton.setOpacity(0.5);
                        takeOfferButton.setOnAction(e -> controller.onRemoveOffer(item.getOffer()));
                    } else {
                        takeOfferButton.setText(item.getTakeOfferButtonText());
                        takeOfferButton.setOpacity(1);
                        resetStyles();
                        if (item.getOffer().getDirection().mirror().isBuy()) {
                            takeOfferButton.getStyleClass().add("buy-button");
                        } else {
                            takeOfferButton.getStyleClass().add("sell-button");
                        }
                        takeOfferButton.setOnAction(e -> controller.onTakeOffer(item.getOffer()));
                    }
                    setGraphic(takeOfferButton);
                } else {
                    resetStyles();
                    takeOfferButton.setOnAction(null);
                    setGraphic(null);
                }
            }

            private void resetStyles() {
                takeOfferButton.getStyleClass().remove("buy-button");
                takeOfferButton.getStyleClass().remove("sell-button");
                takeOfferButton.getStyleClass().remove("white-transparent-outlined-button");
            }
        };
    }

    private void selectedMarketItemChanged(MarketItem selectedItem) {
        marketListView.getSelectionModel().clearSelection();
        marketListView.getSelectionModel().select(selectedItem);
        favouritesListView.getSelectionModel().clearSelection();
        favouritesListView.getSelectionModel().select(selectedItem);

        if (selectedItem != null) {
            Node baseMarketImage = MarketImageComposition.createMarketLogo(model.getMarketIconId().get());
            marketListTitle.setGraphic(baseMarketImage);

            // TODO: This now needs to take into account the base market as well
            if (marketHeaderIcon != null) {
                StackPane tradePairImage = MarketImageComposition.getMarketIcons(selectedItem.getMarket(), Optional.empty());
                marketHeaderIcon.setGraphic(tradePairImage);
            }
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
        marketTitle.getStyleClass().addAll("chat-header-title", "offerbook-channel-title");
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
        Button createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        return createOfferButton;
    }

    private void setupOffersVBox() {
        allOffersToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerlistSubheader.offersToggleGroup.allOffers"));
        allOffersToggleButton.getStyleClass().add("offerlist-toggle-button-all-offers");
        buyToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerlistSubheader.offersToggleGroup.buy"));
        buyToggleButton.getStyleClass().add("offerlist-toggle-button-buy");
        sellToggleButton = new ToggleButton(Res.get("muSig.offerbook.offerlistSubheader.offersToggleGroup.sell"));
        sellToggleButton.getStyleClass().add("offerlist-toggle-button-sell");
        offerlistFiltersToggleGroup = new ToggleGroup();
        offerlistFiltersToggleGroup.getToggles().addAll(allOffersToggleButton, buyToggleButton, sellToggleButton);
        HBox toggleButtonHBox = new HBox(3, allOffersToggleButton, buyToggleButton, sellToggleButton);
        toggleButtonHBox.getStyleClass().add("mu-sig-offerbook-offerlist-toggle-button-hbox");

        HBox subheaderContent = new HBox(toggleButtonHBox, Spacer.fillHBox()/*, messageTypeFilterMenu*/);
        subheaderContent.getStyleClass().add("mu-sig-offerbook-subheader-content");
        subheaderContent.setPadding(new Insets(0, 12, 0, 13));
        HBox.setHgrow(subheaderContent, Priority.ALWAYS);

        HBox subheader = new HBox(subheaderContent);
        subheader.getStyleClass().add("offerbook-subheader");
        subheader.setAlignment(Pos.CENTER);

        VBox.setMargin(subheader, new Insets(0, 0, 5, 0));
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
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), MarketFilter.WITH_OFFERS);
        filterFavourites = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites"), MarketFilter.FAVOURITES);
        filterShowAll = new SortAndFilterDropdownMenuItem<>("check-white", "check-white",
                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.all"), MarketFilter.ALL);

        dropdownMenu.addMenuItems(sortTitle, sortByMostOffers, sortByNameAZ, sortByNameZA, separator, filterTitle,
                filterWithOffers, filterFavourites, filterShowAll);
        return dropdownMenu;
    }

    private void updateSelectedMarketFilter(MarketFilter bisqEasyMarketFilter) {
        if (bisqEasyMarketFilter == null) {
            return;
        }

        //noinspection unchecked
        sortAndFilterMarketsMenu.getMenuItems().stream()
                .filter(menuItem -> menuItem instanceof SortAndFilterDropdownMenuItem &&
                        ((SortAndFilterDropdownMenuItem<?>) menuItem).getMenuItem() instanceof MarketFilter)
                .map(menuItem -> (SortAndFilterDropdownMenuItem<MarketFilter>) menuItem)
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

    private void updateSelectedOfferlistFilter(@Nullable Direction direction) {
        if (direction == null) {
            allOffersToggleButton.setSelected(true);
        } else if (direction.isBuy()) {
            sellToggleButton.setSelected(true);
        } else { // direction.isSell()
            buyToggleButton.setSelected(true);
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
}
