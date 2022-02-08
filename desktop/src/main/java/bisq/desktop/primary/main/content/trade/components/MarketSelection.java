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

package bisq.desktop.primary.main.content.trade.components;

import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MarketSelection {
    private final MarketSelectionController controller;

    public MarketSelection(SettingsService settingsService) {
        controller = new MarketSelectionController(settingsService);
    }

    public ReadOnlyObjectProperty<Market> selectedMarketProperty() {
        return controller.model.selectedMarket;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSelectedMarket(Market market) {
        controller.model.selectedMarket.set(market);
    }

    private static class MarketSelectionController implements Controller {
        private final MarketSelectionModel model;
        @Getter
        private final MarketSelectionView view;

        private MarketSelectionController(SettingsService settingsService) {
            model = new MarketSelectionModel(settingsService);
            view = new MarketSelectionView(model, this);
        }

        @Override
        public void onViewAttached() {
            model.markets.setAll(model.settingsService.getMarkets());
            model.selectedMarket.set(model.settingsService.getSelectedMarket());
        }

        @Override
        public void onViewDetached() {
        }

        private void onSelectMarket(Market selected) {
            if (selected != null) {
                model.selectedMarket.set(selected);
            }
        }
    }

    private static class MarketSelectionModel implements Model {
        private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
        private final ObservableList<Market> markets = FXCollections.observableArrayList();
        private final SettingsService settingsService;

        public MarketSelectionModel(SettingsService settingsService) {
            this.settingsService = settingsService;
        }
    }

    @Slf4j
    public static class MarketSelectionView extends View<VBox, MarketSelectionModel, MarketSelectionController> {
        private final BisqComboBox<Market> comboBox;
        private final ChangeListener<Market> selectedMarketListener;

        private MarketSelectionView(MarketSelectionModel model, MarketSelectionController controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new BisqLabel(Res.get("createOffer.selectMarket"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            comboBox = new BisqComboBox<>();
            comboBox.setVisibleRowCount(12);
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
            selectedMarketListener = (o, old, newValue) -> comboBox.getSelectionModel().select(newValue);
        }

        @Override
        public void onViewAttached() {
            comboBox.setOnAction(e -> controller.onSelectMarket(comboBox.getSelectionModel().getSelectedItem()));
            model.selectedMarket.addListener(selectedMarketListener);
            comboBox.getSelectionModel().select(model.selectedMarket.get());
        }

        @Override
        public void onViewDetached() {
            comboBox.setOnAction(null);
            model.selectedMarket.addListener(selectedMarketListener);
        }
    }
}