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
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

@Slf4j
public final class MuSigOfferbookView extends View<VBox, MuSigOfferbookModel, MuSigOfferbookController> {
    private final static double HEADER_HEIGHT = 61;
    private static final double LIST_CELL_HEIGHT = 53;
    private static final double MARKET_LIST_WIDTH = 210;
    private static final double SIDE_PADDING = 40;

    private final RichTableView<MuSigOfferListItem> muSigOfferListView;
    private final HBox titleHBox = new HBox(10);
    private VBox marketListVBox;
    private Label marketListTitle, marketHeaderIcon, marketTitle, marketDescription, marketPrice;
    private BisqTableView<MarketItem> marketListView;
    private Subscription selectedMarketItemPin, marketListViewSelectionPin;

    public MuSigOfferbookView(MuSigOfferbookModel model, MuSigOfferbookController controller) {
        super(new VBox(), model, controller);

        muSigOfferListView = new RichTableView<>(model.getSortedMuSigOfferListItems());
        muSigOfferListView.getFooterVBox().setVisible(false);
        muSigOfferListView.getFooterVBox().setManaged(false);
        configMuSigOfferListView();
        createAndconfigMarketListView();
        configTitleHBox();

        VBox centerVBox = new VBox(titleHBox, Layout.hLine(), /*subheader, */muSigOfferListView);
        VBox.setVgrow(muSigOfferListView, Priority.ALWAYS);
        VBox.setVgrow(marketListVBox, Priority.ALWAYS);
        centerVBox.getStyleClass().add("bisq-easy-container");
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        VBox.setVgrow(centerVBox, Priority.ALWAYS);

        HBox marketAndOfferListHBox = new HBox(20, marketListVBox, centerVBox);
        VBox.setVgrow(marketAndOfferListHBox, Priority.ALWAYS);

        root.getChildren().add(marketAndOfferListHBox);
        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
    }

    @Override
    protected void onViewAttached() {
        muSigOfferListView.initialize();
        muSigOfferListView.resetSearch();
        muSigOfferListView.sort();

        marketListView.initialize();

        marketTitle.textProperty().bind(model.getMarketTitle());
        marketDescription.textProperty().bind(model.getMarketDescription());
        marketPrice.textProperty().bind(model.getMarketPrice());

        selectedMarketItemPin = EasyBind.subscribe(model.getSelectedMarketItem(), this::selectedMarketItemChanged);
        marketListViewSelectionPin = EasyBind.subscribe(marketListView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                controller.onSelectMarketItem(item);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        muSigOfferListView.dispose();

        marketTitle.textProperty().unbind();
        marketDescription.textProperty().unbind();
        marketPrice.textProperty().unbind();

        selectedMarketItemPin.unsubscribe();
        marketListViewSelectionPin.unsubscribe();
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

    private void createAndconfigMarketListView() {
        marketListTitle = new Label(Res.get("bisqEasy.offerbook.markets"));
        HBox.setHgrow(marketListTitle, Priority.ALWAYS);

        HBox header = new HBox(marketListTitle);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 12, 0, 12));
        header.getStyleClass().add("chat-header-title");

//        marketSelectorSearchBox = new SearchBox();
//        marketSelectorSearchBox.getStyleClass().add("offerbook-search-box");
//        sortAndFilterMarketsMenu = createAndGetSortAndFilterMarketsMenu();
//        HBox subheader = new HBox(marketSelectorSearchBox, Spacer.fillHBox(), sortAndFilterMarketsMenu);
//        subheader.setAlignment(Pos.CENTER);
//        subheader.getStyleClass().add("market-selection-subheader");
//
//        withOffersRemoveFilterDefaultIcon = ImageUtil.getImageViewById("close-mini-grey");
//        withOffersRemoveFilterActiveIcon = ImageUtil.getImageViewById("close-mini-white");
//        removeWithOffersFilter = createAndGetRemoveFilterLabel(withOffersRemoveFilterDefaultIcon);
//        withOffersDisplayHint = createAndGetDisplayHintHBox(
//                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.withOffers"), removeWithOffersFilter);
//
//        favouritesRemoveFilterDefaultIcon = ImageUtil.getImageViewById("close-mini-grey");
//        favouritesRemoveFilterActiveIcon = ImageUtil.getImageViewById("close-mini-white");
//        removeFavouritesFilter = createAndGetRemoveFilterLabel(favouritesRemoveFilterDefaultIcon);
//        onlyFavouritesDisplayHint = createAndGetDisplayHintHBox(
//                Res.get("bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites"), removeFavouritesFilter);
//
//        appliedFiltersSection = new HBox(withOffersDisplayHint, onlyFavouritesDisplayHint);
//        appliedFiltersSection.setAlignment(Pos.CENTER_RIGHT);
//        HBox.setHgrow(appliedFiltersSection, Priority.ALWAYS);
//
//        favouritesTableView = new BisqTableView<>(getModel().getFavouriteMarketChannelItems());
//        favouritesTableView.getStyleClass().addAll("market-selection-list", "favourites-list");
//        favouritesTableView.hideVerticalScrollbar();
//        favouritesTableView.hideHorizontalScrollbar();
//        favouritesTableView.setFixedCellSize(LIST_CELL_HEIGHT);
//        configMarketsTableView(favouritesTableView);

        marketListView = new BisqTableView<>(model.getSortedMarketItems(), false);
        marketListView.getStyleClass().addAll("market-selection-list", "markets-list");
        marketListView.allowVerticalScrollbar();
        marketListView.hideHorizontalScrollbar();
        marketListView.setFixedCellSize(LIST_CELL_HEIGHT);
        marketListView.setPlaceholder(new Label());
        configMarketsTableView(marketListView);
        VBox.setVgrow(marketListView, Priority.ALWAYS);

        marketListVBox = new VBox(header, Layout.hLine(), /*subheader, appliedFiltersSection, favouritesTableView,*/
                marketListView);
        marketListVBox.setMaxWidth(MARKET_LIST_WIDTH);
        marketListVBox.setPrefWidth(MARKET_LIST_WIDTH);
        marketListVBox.setMinWidth(MARKET_LIST_WIDTH);
        marketListVBox.setFillWidth(true);
        marketListVBox.getStyleClass().add("chat-container");
//        HBox.setMargin(marketListVBox, new Insets(1, 0, 0, 0));
    }

    private void configMarketsTableView(BisqTableView<MarketItem> tableView) {
        BisqTableColumn<MarketItem> marketLogoTableColumn = new BisqTableColumn.Builder<MarketItem>()
                .fixWidth(55)
                .setCellFactory(getMarketLogoCellFactory())
                .isSortable(false)
                .build();

        BisqTableColumn<MarketItem> marketLabelTableColumn = new BisqTableColumn.Builder<MarketItem>()
                .minWidth(100)
                .left()
                .setCellFactory(getMarketLabelCellFactory(false))
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
                    numOffers.setText(getFormattedOfferNumber(item.getNumOffers().get()));
                    String quoteCurrencyDisplayName = StringUtils.capitalize(item.getMarket().getQuoteCurrencyDisplayName());
                    marketDetailsTooltip.setText(getFormattedTooltip(item.getNumOffers().get(), quoteCurrencyDisplayName));
                    marketName.setText(quoteCurrencyDisplayName);
                    marketCode.setText(item.getMarket().getQuoteCurrencyCode());
                    favouritesLabel.setOnMouseClicked(e -> item.toggleFavourite());
                    setGraphic(container);
                } else {
                    favouritesLabel.setOnMouseClicked(null);
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
//                        takeOfferButton.setOnAction(e -> controller.onRemoveOffer(item.getOffer()));
                    } else {
                        takeOfferButton.setText(item.getTakeOfferButtonText());
                        takeOfferButton.setOpacity(1);
                        resetStyles();
                        if (item.getOffer().getDirection().mirror().isBuy()) {
                            takeOfferButton.getStyleClass().add("buy-button");
                        } else {
                            takeOfferButton.getStyleClass().add("sell-button");
                        }
//                        takeOfferButton.setOnAction(e -> controller.onTakeOffer(item.getOffer()));
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
//        favouritesTableView.getSelectionModel().clearSelection();
//        favouritesTableView.getSelectionModel().select(selectedItem);

        if (selectedItem != null && marketHeaderIcon != null) {
            // TODO: This now needs to take into account the base market as well
            StackPane marketsImage = MarketImageComposition.getMarketIcons(selectedItem.getMarket(), Optional.empty());
            marketHeaderIcon.setGraphic(marketsImage);
        }
    }

    private void configTitleHBox() {
        titleHBox.getStyleClass().add("chat-container-header");

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

//        createOfferButton = createAndGetCreateOfferButton();
//        createOfferButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);

        HBox.setHgrow(headerTitle, Priority.ALWAYS);
        titleHBox.getChildren().setAll(headerTitle/*, createOfferButton, ellipsisMenu, notificationsSettingsMenu*/);
    }
}
