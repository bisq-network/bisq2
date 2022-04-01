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

package bisq.desktop.primary.main.top.components;

import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqComboBox;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * We pack the MVC classes directly into the Component class to have it more compact as scope and complexity is
 * rather limited.
 * <p>
 * Is never removed so no need to handle onViewDetached case
 */
@Slf4j
public class MarketPriceComponent {
    private final Controller controller;

    public MarketPriceComponent(MarketPriceService marketPriceService) {
        controller = new Controller(marketPriceService);
    }

    public Pane getRootPane() {
        return controller.getView().getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller, MarketPriceService.Listener {
        private final MarketPriceService marketPriceService;
        private final Model model;
        @Getter
        private final View view;

        private Controller(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
            model = new Model();
            view = new View(model, this);
            marketPriceService.addListener(this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(() -> model.applyMarketPriceMap(map));
        }

        @Override
        public void onMarketPriceSelected(MarketPrice selected) {
            UIThread.run(() -> model.selected.set(new ListItem(selected)));
        }

        private void onSelect(ListItem selectedItem) {
            if (selectedItem != null) {
                marketPriceService.select(selectedItem.marketPrice);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<ListItem> items = FXCollections.observableArrayList();
        private final ObjectProperty<ListItem> selected = new SimpleObjectProperty<>();

        private void applyMarketPriceMap(Map<Market, MarketPrice> map) {
            //todo use preferred currencies + edit entry
            List<ListItem> list = map.values().stream().map(ListItem::new).collect(Collectors.toList());
            items.setAll(list);
            if (selected.get() != null) {
                selected.set(new ListItem(map.get(selected.get().marketPrice.getMarket())));
            }
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, bisq.desktop.common.view.Model, bisq.desktop.common.view.Controller> {
        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setAlignment(Pos.CENTER_LEFT);

            ComboBox<ListItem> comboBox = new BisqComboBox<>();
            comboBox.setVisibleRowCount(12);
            comboBox.setFocusTraversable(false);
            comboBox.setId("price-feed-combo");
            comboBox.setPadding(new Insets(0, -4, -4, 0));
            comboBox.setItems(model.items);
            comboBox.setOnAction(e -> controller.onSelect(comboBox.getSelectionModel().getSelectedItem()));
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable ListItem listItem) {
                    return listItem != null ? listItem.displayStringProperty.get() : "";
                }

                @Override
                public ListItem fromString(String string) {
                    return null;
                }
            });

            Label marketPriceLabel = new Label();
            marketPriceLabel.getStyleClass().add("nav-balance-label");
            marketPriceLabel.setPadding(new Insets(-2, 0, 4, 9));
            //todo add provider info to marketPriceLabel

            root.getChildren().addAll(comboBox, marketPriceLabel);

            model.selected.addListener((o, old, newValue) -> comboBox.getSelectionModel().select(newValue));
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }

    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ListItem {
        private final StringProperty displayStringProperty = new SimpleStringProperty();
        private final MarketPrice marketPrice;
        @EqualsAndHashCode.Include
        private final String code;

        private ListItem(MarketPrice marketPrice) {
            this.marketPrice = marketPrice;
            code = marketPrice.code();
            String pair = TradeCurrency.isFiat(code) ? ("BTC/" + code) : (code + "/BTC");
            displayStringProperty.set(pair + ": " + QuoteFormatter.format(marketPrice.quote(), true));
        }
    }
}