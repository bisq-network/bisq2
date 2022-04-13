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

package bisq.desktop.components.controls;

import bisq.common.monetary.Market;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MarketSelection {
    private final Controller controller;

    public MarketSelection(SettingsService settingsService) {
        controller = new Controller(settingsService);
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

    public void setPrefWidth(double prefWidth) {
        controller.model.prefWidth.set(prefWidth);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(SettingsService settingsService) {
            model = new Model(settingsService);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.markets.setAll(model.settingsService.getMarkets());
            model.selectedMarket.set(model.settingsService.getSelectedMarket());
        }

        @Override
        public void onDeactivate() {
        }

        private void onSelectMarket(Market selected) {
            if (selected != null) {
                model.selectedMarket.set(selected);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Market> selectedMarket = new SimpleObjectProperty<>();
        private final ObservableList<Market> markets = FXCollections.observableArrayList();
        private final SettingsService settingsService;
        private final DoubleProperty prefWidth = new SimpleDoubleProperty(250);

        public Model(SettingsService settingsService) {
            this.settingsService = settingsService;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqComboBox<Market> comboBox;
        private final ChangeListener<Market> selectedMarketListener;
        private final ListChangeListener<Market> marketsListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            root.setMaxWidth(model.prefWidth.get());

            comboBox = new BisqComboBox<>();
            comboBox.setDescription(Res.get("markets"));
            comboBox.setPrefWidth(model.prefWidth.get());
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

            root.getChildren().addAll(comboBox.getRoot());

            // From model
            selectedMarketListener = (o, old, newValue) -> comboBox.selectItem(newValue);

            marketsListener = c -> comboBox.setItems(model.markets);
        }

        @Override
        protected void onViewAttached() {
            comboBox.setOnAction(() -> controller.onSelectMarket(comboBox.getSelectedItem()));
            model.selectedMarket.addListener(selectedMarketListener);
            comboBox.selectItem(model.selectedMarket.get());
            model.markets.addListener(marketsListener);
            comboBox.setItems(model.markets);
        }

        @Override
        protected void onViewDetached() {
            comboBox.setOnAction(null);
            model.selectedMarket.removeListener(selectedMarketListener);
            model.markets.removeListener(marketsListener);
        }
    }
}