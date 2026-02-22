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

package bisq.desktop.main.top;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceRequestService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.network.Address;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownListMenu;
import bisq.desktop.components.controls.ProgressBarWithLabel;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import bisq.network.http.HttpRequestUrlProvider;
import bisq.network.identity.NetworkId;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.presentation.formatters.TimeFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class MarketPriceComponent {
    private final Controller controller;

    public MarketPriceComponent(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final MarketPriceService marketPriceService;
        private Pin selectedMarketPin, marketPricePin;

        private Controller(ServiceProvider serviceProvider) {
            marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                    UIThread.run(() -> {
                        List<ListItem> list = MarketRepository.getAllFiatMarkets().stream()
                                .flatMap(market -> marketPriceService.findMarketPrice(market).stream())
                                .map(marketPrice -> new ListItem(marketPrice, marketPriceService))
                                .collect(Collectors.toList());
                        model.items.setAll(list);

                        // We use the model.items in the selectedMarket handler code, so we only start the observer 
                        // registration once we got the list.
                        if (selectedMarketPin != null) {
                            selectedMarketPin.unbind();
                        }
                        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket ->
                                UIThread.run(() -> {
                                    if (selectedMarket != null) {
                                        model.items.stream()
                                                .filter(item -> item.marketPrice.getMarket().equals(selectedMarket))
                                                .findAny()
                                                .ifPresent(listItem -> {
                                                    model.market.set(listItem.market);
                                                    model.codes.set(listItem.codes);
                                                    model.price.set(listItem.price);
                                                    model.selected.set(listItem);
                                                });
                                    }
                                }));
                    }));
            applyFilteredListItems();
        }

        @Override
        public void onDeactivate() {
            marketPricePin.unbind();
            if (selectedMarketPin != null) {
                selectedMarketPin.unbind();
            }
        }

        private void onSelected(MarketPriceComponent.ListItem selectedItem) {
            if (selectedItem != null) {
                marketPriceService.setSelectedMarket(selectedItem.marketPrice.getMarket());
            }
        }

        private void applySearchPredicate(String searchText) {
            String string = searchText == null ? "" : searchText.toLowerCase();
            model.setSearchStringPredicate(item -> item.getCodes().toLowerCase().contains(string));
            applyFilteredListItems();
        }

        private void applyFilteredListItems() {
            model.filteredItems.setPredicate(null);
            model.filteredItems.setPredicate(model.searchStringPredicate);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<ListItem> items = FXCollections.observableArrayList();
        private final FilteredList<ListItem> filteredItems = new FilteredList<>(items);
        private final ObjectProperty<ListItem> selected = new SimpleObjectProperty<>();
        private final ObjectProperty<Market> market = new SimpleObjectProperty<>();
        private final StringProperty codes = new SimpleStringProperty();
        private final StringProperty price = new SimpleStringProperty();
        @Setter
        private Predicate<ListItem> searchStringPredicate = item -> true;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private static final double LIST_MENU_CELL_HEIGHT = 50;
        private static final double LIST_MENU_WIDTH = 290;
        private static final Map<String, StackPane> MARKET_IMAGE_CACHE = new HashMap<>();

        private final Label codes, price;
        private final ProgressBarWithLabel progressBarWithLabel;
        private final Tooltip tooltip;
        private final Label staleIcon;
        private final DropdownListMenu<ListItem> listMenu;
        private Subscription marketPin, pricePin;
        private UIScheduler updateScheduler;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setAlignment(Pos.CENTER);
            root.setCursor(Cursor.HAND);

            codes = new Label();
            codes.setMouseTransparent(true);
            codes.setGraphicTextGap(7);
            codes.getStyleClass().add("bisq-text-18");

            price = new Label();
            price.setMouseTransparent(true);
            price.getStyleClass().add("bisq-text-19");

            progressBarWithLabel = new ProgressBarWithLabel(Res.get("component.marketPrice.requesting"));

            tooltip = new BisqTooltip();
            Tooltip.install(root, tooltip);

            ImageView icon = ImageUtil.getImageViewById("undelivered-message-grey");
            staleIcon = new Label();
            staleIcon.setGraphic(icon);
            staleIcon.setManaged(false);
            staleIcon.setVisible(false);

            listMenu = new DropdownListMenu<>("chevron-drop-menu-grey",
                    "chevron-drop-menu-white", false, model.filteredItems,
                    controller::applySearchPredicate);
            HBox menu = new HBox(5, codes, price, progressBarWithLabel, staleIcon);
            menu.setAlignment(Pos.CENTER);
            listMenu.setContent(menu);
            listMenu.getTableView().setFixedCellSize(LIST_MENU_CELL_HEIGHT);
            listMenu.getTableView().setPrefWidth(LIST_MENU_WIDTH);
            configTableView();

            root.getStyleClass().add("market-price-component");
            root.getChildren().add(listMenu);
        }

        @Override
        protected void onViewAttached() {
            listMenu.initialize();

            codes.textProperty().bind(model.codes);

            marketPin = EasyBind.subscribe(model.market, market -> {
                if (market != null) {
                    codes.setGraphic(MarketImageComposition.getMarketMenuPairIcons(
                            market.getBaseCurrencyCode(), market.getQuoteCurrencyCode()));
                }
            });

            pricePin = EasyBind.subscribe(model.price, priceValue -> {
                boolean isPriceSet = StringUtils.isNotEmpty(priceValue);
                progressBarWithLabel.setVisible(!isPriceSet);
                progressBarWithLabel.setManaged(!isPriceSet);
                progressBarWithLabel.setProgress(isPriceSet ? 0 : -1);
                price.setText(isPriceSet ? priceValue : "");
            });

            updateScheduler = UIScheduler.run(() -> {
                        ListItem item = model.selected.get();
                        if (item == null) {
                            return;
                        }
                        boolean isStale = item.isStale();
                        staleIcon.setManaged(isStale);
                        staleIcon.setVisible(isStale);
                        String isStalePostFix = isStale ? Res.get("component.marketPrice.tooltip.isStale") : "";
                        tooltip.setText(Res.get("component.marketPrice.tooltip",
                                item.getSource(),
                                item.getAgeInSeconds(),
                                item.date,
                                isStalePostFix));
                    })
                    .periodically(1000);
        }

        @Override
        protected void onViewDetached() {
            listMenu.dispose();

            codes.textProperty().unbind();

            marketPin.unsubscribe();
            pricePin.unsubscribe();

            Tooltip.uninstall(root, tooltip);
            updateScheduler.stop();
        }

        private void configTableView() {
            listMenu.getTableView().getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .left()
                    .setCellFactory(getListCell())
                    .build());
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getListCell() {
            return column -> new TableCell<>() {
                private final Label price, codes;
                private final HBox hBox = new HBox(10);
                private final Tooltip tooltip;
                private final Label staleIcon;

                {
                    setCursor(Cursor.HAND);

                    codes = new Label();
                    codes.setMouseTransparent(true);
                    codes.setGraphicTextGap(10);
                    codes.setStyle("-fx-text-fill: -fx-mid-text-color;");

                    price = new Label();
                    price.setMouseTransparent(true);
                    price.setStyle("-fx-text-fill: -fx-light-text-color;");

                    ImageView icon = ImageUtil.getImageViewById("undelivered-message-grey");
                    staleIcon = new Label();
                    staleIcon.setGraphic(icon);
                    staleIcon.setVisible(false);

                    hBox.getChildren().addAll(codes, price, staleIcon);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    tooltip = new BisqTooltip();
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        price.setText(item.price);
                        codes.setText(item.codes);

                        StackPane marketsImage = MarketImageComposition.getMarketMenuIcons(item.market, MARKET_IMAGE_CACHE);
                        marketsImage.setCache(true);
                        marketsImage.setCacheHint(CacheHint.SPEED);
                        codes.setGraphic(marketsImage);

                        boolean isStale = item.isStale();
                        staleIcon.setVisible(isStale);
                        tooltip.setText(item.getTooltipText());
                        Tooltip.install(hBox, tooltip);
                        setOnMouseClicked(e -> controller.onSelected(item));
                        setGraphic(hBox);
                    } else {
                        Tooltip.uninstall(hBox, tooltip);
                        setOnMouseClicked(null);
                        setGraphic(null);
                    }
                }
            };
        }
    }

    @Slf4j
    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ListItem {
        @EqualsAndHashCode.Include
        private final MarketPrice marketPrice;

        private final Market market;
        private final String codes;
        private final String price;
        private final String date;
        private final MarketPriceService marketPriceService;

        private ListItem(MarketPrice marketPrice, MarketPriceService marketPriceService) {
            this.marketPrice = marketPrice;
            this.marketPriceService = marketPriceService;

            market = marketPrice.getMarket();
            codes = market.getMarketCodes();
            price = PriceFormatter.format(marketPrice.getPriceQuote(), true);
            date = DateFormatter.formatDateTime(marketPrice.getTimestamp());
        }

        public boolean isStale() {
            return marketPrice.isStale();
        }

        public String getAgeInSeconds() {
            return TimeFormatter.getAgeInSeconds(marketPrice.getAge());
        }

        public String getTooltipText() {
            String isStalePostFix = isStale() ? Res.get("component.marketPrice.tooltip.isStale") : "";
            return Res.get("component.marketPrice.tooltip",
                    getSource(),
                    getAgeInSeconds(),
                    date,
                    isStalePostFix);
        }

        public String getProviderUrl() {
            return marketPriceService.getMarketPriceRequestService()
                    .flatMap(MarketPriceRequestService::getMostRecentProvider)
                    .map(HttpRequestUrlProvider::getBaseUrl)
                    .orElseGet(() -> Res.get("data.na"));
        }

        public String getMarketPriceProvidingOracle() {
            return marketPriceService.getMarketPriceProvidingOracle()
                    .map(AuthorizedBondedRole::getNetworkId)
                    .map(NetworkId::getAddressByTransportTypeMap)
                    .flatMap(map -> map.values().stream().findAny())
                    .map(Address::getFullAddress)
                    .orElseGet(() -> Res.get("data.na"));
        }

        public String getSource() {
            MarketPrice.Source source = marketPrice.getSource();
            return switch (source) {
                case PERSISTED -> Res.get("component.marketPrice.source." + source);
                case PROPAGATED_IN_NETWORK ->
                        Res.get("component.marketPrice.source." + source, getMarketPriceProvidingOracle());
                case REQUESTED_FROM_PRICE_NODE -> Res.get("component.marketPrice.source." + source, getProviderUrl());
            };
        }

        @Override
        public String toString() {
            return codes + " " + price;
        }
    }
}