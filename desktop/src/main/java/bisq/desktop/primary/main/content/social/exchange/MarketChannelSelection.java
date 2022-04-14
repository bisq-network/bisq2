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

package bisq.desktop.primary.main.content.social.exchange;

import bisq.common.monetary.Market;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;

@Slf4j
public class MarketChannelSelection {
    private final Controller controller;

    public MarketChannelSelection(SettingsService settingsService) {
        controller = new Controller(settingsService);
    }

    public ReadOnlyObjectProperty<MarketChannelItem> selectedMarketProperty() {
        return controller.model.selectedMarketChannelItem;
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSelectedMarket(MarketChannelItem market) {
        controller.model.selectedMarketChannelItem.set(market);
    }

    public void setPrefWidth(double prefWidth) {
        controller.model.prefWidth.set(prefWidth);
    }

    public void setCellFactory(Callback<ListView<MarketChannelItem>, ListCell<MarketChannelItem>> value) {
        controller.model.cellFactory = Optional.of(value);
    }

    public void setButtonCell(ListCell<MarketChannelItem> value) {
        controller.model.buttonCell = Optional.of(value);
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
            FxBindings.<Market, MarketChannelItem>bind(model.marketChannelItems)
                    .map(MarketChannelItem::new)
                    .to(model.settingsService.getMarkets());
            // model.marketChannelItems.setAll(model.settingsService.getMarkets());
            //  model.selectedMarketChannelItem.set(model.settingsService.getSelectedMarket());
        }

        @Override
        public void onDeactivate() {
        }

        private void onSelectMarket(MarketChannelItem selected) {
            if (selected != null) {
                model.selectedMarketChannelItem.set(selected);
            }
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<MarketChannelItem> selectedMarketChannelItem = new SimpleObjectProperty<>();
        private final ObservableList<MarketChannelItem> marketChannelItems = FXCollections.observableArrayList();
        private final SettingsService settingsService;
        private final DoubleProperty prefWidth = new SimpleDoubleProperty(250);
        private Optional<Callback<ListView<MarketChannelItem>, ListCell<MarketChannelItem>>> cellFactory = Optional.empty();
        private Optional<ListCell<MarketChannelItem>> buttonCell = Optional.empty();

        public Model(SettingsService settingsService) {
            this.settingsService = settingsService;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqComboBox<MarketChannelItem> comboBox;
        private final ChangeListener<MarketChannelItem> selectedMarketListener;
        private final ListChangeListener<MarketChannelItem> marketsListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

           // root.setPrefWidth(model.prefWidth.get());

            comboBox = new BisqComboBox<>();
            comboBox.setDescription(Res.get("social.marketChannels"));
          //  comboBox.setPrefWidth(model.prefWidth.get());
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable MarketChannelItem value) {
                    return value != null ? value.toString() : "";
                }

                @Override
                public MarketChannelItem fromString(String string) {
                    return null;
                }
            });

            root.getChildren().addAll(comboBox.getRoot());

            // From model
            selectedMarketListener = (o, old, newValue) -> comboBox.selectItem(newValue);

            marketsListener = c -> comboBox.setItems(model.marketChannelItems);
        }

        @Override
        protected void onViewAttached() {
            model.cellFactory.ifPresent(comboBox::setCellFactory);
            comboBox.setOnAction(() -> controller.onSelectMarket(comboBox.getSelectedItem()));
            model.selectedMarketChannelItem.addListener(selectedMarketListener);
            comboBox.selectItem(model.selectedMarketChannelItem.get());
            model.marketChannelItems.addListener(marketsListener);
            comboBox.setItems(model.marketChannelItems);
            comboBox.getRoot().prefWidthProperty().bind(root.widthProperty());
        }

        @Override
        protected void onViewDetached() {
            comboBox.setOnAction(null);
            model.selectedMarketChannelItem.removeListener(selectedMarketListener);
            model.marketChannelItems.removeListener(marketsListener);
        }
    }

    @Getter
    public final static class MarketChannelItem {
        private final Market market;
        private int numMessages = new Random().nextInt(100);

        public MarketChannelItem(Market market) {
            this.market = market;
        }

        @Override
        public String toString() {
            return market.toString();
        }
    }
}