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

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ProgressBarWithLabel;
import bisq.desktop.components.overlay.ComboBoxWithSearch;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.presentation.formatters.TimeFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MarketPriceComponent {
    private final Controller controller;

    public MarketPriceComponent(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
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

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                    UIThread.run(() -> {
                        List<ListItem> list = MarketRepository.getAllFiatMarkets().stream()
                                .flatMap(market -> marketPriceService.findMarketPrice(market).stream())
                                .map(ListItem::new)
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
                                                    model.price.set(listItem.price);
                                                    model.codes.set(listItem.codes);
                                                    model.selected.set(listItem);
                                                });
                                    }
                                }));
                    }));
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
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<MarketPriceComponent.ListItem> items = FXCollections.observableArrayList();
        private final ObjectProperty<MarketPriceComponent.ListItem> selected = new SimpleObjectProperty<>();
        private final StringProperty codes = new SimpleStringProperty();
        private final StringProperty price = new SimpleStringProperty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label codes, price;
        private final ImageView arrow;
        private final ProgressBarWithLabel progressBarWithLabel;
        private final Tooltip tooltip;
        private Subscription pricePin;

        private View(Model model, Controller controller) {
            super(new HBox(7), model, controller);

            root.setAlignment(Pos.CENTER);
            root.setCursor(Cursor.HAND);

            codes = new Label();
            codes.setMouseTransparent(true);
            codes.getStyleClass().add("bisq-text-18");

            price = new Label();
            price.setMouseTransparent(true);
            price.getStyleClass().add("bisq-text-19");

            progressBarWithLabel = new ProgressBarWithLabel(Res.get("component.marketPrice.requesting"));

            arrow = ImageUtil.getImageViewById("arrow-down");
            arrow.setMouseTransparent(true);
            arrow.setVisible(false);
            arrow.setManaged(false);

            tooltip = new BisqTooltip();
            Tooltip.install(root, tooltip);

            HBox.setMargin(progressBarWithLabel, new Insets(2.5, 0, 0, 0));
            HBox.setMargin(codes, new Insets(0, 5, 0, 0));
            root.getChildren().addAll(codes, price, arrow, progressBarWithLabel);
        }

        @Override
        protected void onViewAttached() {
            codes.textProperty().bind(model.codes);
            pricePin = EasyBind.subscribe(model.price, priceValue -> {
                boolean isPriceSet = StringUtils.isNotEmpty(priceValue);
                arrow.setVisible(isPriceSet);
                arrow.setManaged(isPriceSet);
                progressBarWithLabel.setVisible(!isPriceSet);
                progressBarWithLabel.setManaged(!isPriceSet);
                progressBarWithLabel.setProgress(isPriceSet ? 0 : -1);
                price.setText(isPriceSet ? priceValue : "");
            });

            root.setOnMouseClicked(e -> {
                if (model.items.isEmpty()) {
                    return;
                }
                new ComboBoxWithSearch<>(root,
                        model.items,
                        c -> getListCell(),
                        controller::onSelected,
                        Res.get("action.search"),
                        null,
                        250, 30, 20, 125)
                        .show();
            });
            root.setOnMouseEntered(e -> {
                ListItem item = model.selected.get();
                tooltip.setText(Res.get("component.marketPrice.tooltip",
                        item.provider,
                        item.source,
                        TimeFormatter.getAgeInSeconds(System.currentTimeMillis() - item.marketPrice.getTimestamp()),
                        item.date));
            });
        }

        @Override
        protected void onViewDetached() {
            codes.textProperty().unbind();
            pricePin.unsubscribe();
            Tooltip.uninstall(root, tooltip);
            root.setOnMouseClicked(null);
            root.setOnMouseEntered(null);
        }

        protected ListCell<ListItem> getListCell() {
            return new ListCell<>() {
                private final Label price, codes;
                private final HBox hBox;
                private final Tooltip tooltip;

                {
                    setCursor(Cursor.HAND);

                    codes = new Label();
                    codes.setMouseTransparent(true);
                    codes.getStyleClass().add("bisq-text-18");
                    HBox.setMargin(codes, new Insets(0, 0, 0, -10));

                    price = new Label();
                    price.setMouseTransparent(true);
                    price.setId("bisq-text-20");

                    hBox = new HBox(12, codes, price);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    setCursor(Cursor.HAND);

                    tooltip = new BisqTooltip();
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        price.setText(item.price);
                        codes.setText(item.codes);
                        Tooltip.install(hBox, tooltip);
                        tooltip.setText(Res.get("component.marketPrice.tooltip",
                                item.provider,
                                item.source,
                                TimeFormatter.getAgeInSeconds(System.currentTimeMillis() - item.marketPrice.getTimestamp()),
                                item.date));

                        setGraphic(hBox);
                    } else {
                        Tooltip.uninstall(hBox, tooltip);
                        setGraphic(null);
                    }
                }
            };
        }
    }

    @Slf4j
    @EqualsAndHashCode
    private static class ListItem {
        private final MarketPrice marketPrice;
        private final String price;
        private final String codes;
        private final String provider;
        private final String date;
        private final String source;

        private ListItem(MarketPrice marketPrice) {
            this.marketPrice = marketPrice;
            codes = marketPrice.getMarket().getMarketCodes();
            price = PriceFormatter.format(marketPrice.getPriceQuote(), true);
            provider = marketPrice.getProviderName();
            source = Res.get("component.marketPrice.source." + marketPrice.getSource());
            date = DateFormatter.formatDateTime(marketPrice.getTimestamp());
        }

        @Override
        public String toString() {
            return codes + " " + price;
        }
    }
}