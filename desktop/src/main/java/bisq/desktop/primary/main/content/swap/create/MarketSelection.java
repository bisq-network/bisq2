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

package bisq.desktop.primary.main.content.swap.create;

import bisq.common.monetary.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * We pack the MVC classes directly into the Component class to have it more compact as scope and complexity is
 * rather limited.
 * <p>
 * Is never removed so no need to handle onViewDetached case
 */
@Slf4j
public class MarketSelection {
    public static class MarketSelectionController implements Controller, MarketPriceService.Listener {
        private final MarketSelectionModel model;
        @Getter
        private final MarketSelectionView view;
        private final MarketPriceService marketPriceService;

        public MarketSelectionController(MarketPriceService marketPriceService, ObjectProperty<Market> selectedMarket) {
            this.marketPriceService = marketPriceService;
            model = new MarketSelectionModel(selectedMarket);
            view = new MarketSelectionView(model, this);
        }

        private void applyMarketPriceDate() {
            if (model.markets.isEmpty()) {
                model.markets.setAll(marketPriceService.getMarketPriceByCurrencyMap().values().stream()
                        .map(MarketPrice::getMarket)
                        .collect(Collectors.toList()));
            }
            if (model.selectedMarket.get() == null) {
                marketPriceService.getSelectedMarketPrice()
                        .ifPresent(marketPrice -> model.selectedMarket.set(marketPrice.getMarket()));
            }

            if (!model.markets.isEmpty() && model.selectedMarket.get() != null) {
                marketPriceService.removeListener(this);
            }
        }

        private void onSelect(Market selected) {
            if (selected != null) {
                model.selectedMarket.set(selected);
            }
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(this::applyMarketPriceDate);
        }

        @Override
        public void onMarketPriceSelected(MarketPrice selected) {
            UIThread.run(this::applyMarketPriceDate);
        }

        public void onViewAttached() {
            marketPriceService.addListener(this);
        }

        public void onViewDetached() {
            marketPriceService.removeListener(this);
        }
    }

    private static class MarketSelectionModel implements Model {
        private final ObservableList<Market> markets = FXCollections.observableArrayList();
        private final ObjectProperty<Market> selectedMarket;

        public MarketSelectionModel(ObjectProperty<Market> selectedMarket) {
            this.selectedMarket = selectedMarket;
        }
    }

    @Slf4j
    public static class MarketSelectionView extends View<VBox, MarketSelectionModel, MarketSelectionController> {
        private final BisqComboBox<Market> comboBox;
        private final ChangeListener<Market> selectedListener;

        public MarketSelectionView(MarketSelectionModel model, MarketSelectionController controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new BisqLabel(Res.offerbook.get("createOffer.selectMarket"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            comboBox = new BisqComboBox<>();
            comboBox.setVisibleRowCount(12);
            comboBox.setFocusTraversable(false);
            comboBox.setId("price-feed-combo");
            comboBox.setPadding(new Insets(0, -4, -4, 0));
            comboBox.setItems(model.markets);
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable Market value) {
                    return value != null ? value.toString() : "";
                }

                @Override
                public Market fromString(String string) {
                    return null;
                }
            });

            root.getChildren().addAll(headline, comboBox);

            // From model
            selectedListener = (o, old, newValue) -> comboBox.getSelectionModel().select(newValue);
        }

        public void onViewAttached() {
            comboBox.setOnAction(e -> controller.onSelect(comboBox.getSelectionModel().getSelectedItem()));
            model.selectedMarket.addListener(selectedListener);
        }

        public void onViewDetached() {
            comboBox.setOnAction(null);
            model.selectedMarket.addListener(selectedListener);
        }
    }
}