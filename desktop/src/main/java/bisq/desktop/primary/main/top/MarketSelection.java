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

package bisq.desktop.primary.main.top;

import bisq.common.currency.MarketRepository;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.overlay.ComboBoxOverlay;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class MarketSelection {
    private final Controller controller;

    public MarketSelection(MarketPriceService marketPriceService) {
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
        private Pin selectedMarketPin, marketPriceUpdateFlagPin;

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> {
                if (selectedMarket != null) {
                    UIThread.run(() -> model.items.stream()
                            .filter(e -> e.marketPrice.getMarket().equals(selectedMarket))
                            .findAny()
                            .ifPresent(listItem -> {
                                model.price.set(listItem.price);
                                model.codes.set(listItem.codes);
                                model.selected.set(listItem);
                            }));
                }
            });

            marketPriceUpdateFlagPin = marketPriceService.getMarketPriceUpdateFlag().addObserver(__ -> {
                UIThread.run(() -> {
                    List<MarketSelection.ListItem> list = MarketRepository.getAllFiatMarkets().stream()
                            .map(market -> marketPriceService.getMarketPriceByCurrencyMap().get(market))
                            .filter(Objects::nonNull)
                            .map(MarketSelection.ListItem::new)
                            .collect(Collectors.toList());
                    model.items.setAll(list);
                });
            });
        }

        @Override
        public void onDeactivate() {
            selectedMarketPin.unbind();
            marketPriceUpdateFlagPin.unbind();
        }

        private void onSelected(MarketSelection.ListItem selectedItem) {
            if (selectedItem != null) {
                marketPriceService.select(selectedItem.marketPrice.getMarket());
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<MarketSelection.ListItem> items = FXCollections.observableArrayList();
        private final ObjectProperty<MarketSelection.ListItem> selected = new SimpleObjectProperty<>();
        private final StringProperty codes = new SimpleStringProperty();
        private final StringProperty price = new SimpleStringProperty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label codes, price;
        private final Node arrow;

        private View(Model model, Controller controller) {
            super(new HBox(7), model, controller);

            root.setAlignment(Pos.CENTER);

            codes = new Label();
            codes.setMouseTransparent(true);
            codes.getStyleClass().add("bisq-text-18");

            price = new Label();
            price.setMouseTransparent(true);
            price.getStyleClass().add("bisq-text-19");

            arrow = ImageUtil.getImageViewById("arrow-down");
            arrow.setMouseTransparent(true);
            HBox.setMargin(codes, new Insets(0, 5, 0, 0));
            root.getChildren().addAll(codes, price, arrow);
        }

        @Override
        protected void onViewAttached() {
            codes.textProperty().bind(model.codes);
            price.textProperty().bind(model.price);
            root.setOnMouseClicked(e ->
                    new ComboBoxOverlay<>(root,
                            model.items,
                            c -> getListCell(),
                            controller::onSelected,
                            Res.get("search"),
                            null,
                            250, 30, 20, 125)
                            .show());
        }

        @Override
        protected void onViewDetached() {
            codes.textProperty().unbind();
            root.setOnMouseClicked(null);
        }

        protected ListCell<ListItem> getListCell() {
            return new ListCell<>() {
                private final Label price, codes;

                {
                    codes = new Label();
                    codes.setMouseTransparent(true);
                    codes.getStyleClass().add("bisq-text-18");
                    price = new Label();
                    price.setMouseTransparent(true);
                    price.setId("bisq-text-20");
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        price.setText(item.price);
                        codes.setText(item.codes);
                        HBox.setMargin(codes, new Insets(0, 0, 0, -10));
                        HBox hBox = new HBox(12, codes, price);
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                    }
                }
            };
        }
    }

    @EqualsAndHashCode
    private static class ListItem {
        private final MarketPrice marketPrice;
        private final String price;
        private final String codes;

        private ListItem(MarketPrice marketPrice) {
            this.marketPrice = marketPrice;
            codes = marketPrice.getMarket().getMarketCodes();
            price = QuoteFormatter.format(marketPrice.getQuote(), true);
        }

        @Override
        public String toString() {
            return codes + " " + price;
        }
    }
}