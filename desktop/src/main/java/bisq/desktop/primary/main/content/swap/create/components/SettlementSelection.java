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

package bisq.desktop.primary.main.content.swap.create.components;

import bisq.account.settlement.Settlement;
import bisq.offer.Direction;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.offer.protocol.ProtocolSpecifics;
import bisq.offer.protocol.SwapProtocolType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
@Getter
public class SettlementSelection {
    public static class SettlementController implements Controller {
        private final SettlementModel model;
        @Getter
        private final SettlementView view;
        private final ChangeListener<SwapProtocolType> selectedProtocolListener;
        private final ChangeListener<Direction> directionListener;
        private final ChangeListener<Market> selectedMarketListener;

        public SettlementController(OfferPreparationModel offerPreparationModel) {
            model = new SettlementModel(offerPreparationModel);
            view = new SettlementView(model, this);

            selectedProtocolListener = (observable, oldValue, newValue) -> resetAndApplyData();
            directionListener = (observable, oldValue, newValue) -> updateStrings();
            selectedMarketListener = (observable, oldValue, newValue) -> resetAndApplyData();
        }

        private void resetAndApplyData() {
            // reset all
            model.setSelectedBaseSideSettlementMethod(null);
            model.setSelectedQuoteSideSettlementMethod(null);

            Direction direction = model.getDirection();
            if (direction == null) return;

            Market market = model.getSelectedMarket();
            if (market == null) return;

            SwapProtocolType selectedProtocolTyp = model.getSelectedProtocolTyp();
            if (selectedProtocolTyp == null) return;

            model.baseSettlementMethods.setAll(ProtocolSpecifics.getSettlementMethods(selectedProtocolTyp,
                    market.baseCurrencyCode()));
            model.quoteSettlementMethods.setAll(ProtocolSpecifics.getSettlementMethods(selectedProtocolTyp,
                    market.quoteCurrencyCode()));

            // Only show if > 1 options
            model.baseSideVisibility.set(model.baseSettlementMethods.size() > 1);
            model.quoteSideVisibility.set(model.quoteSettlementMethods.size() > 1);

            // Auto-select first in list
            if (!model.baseSettlementMethods.isEmpty()) {
                model.setSelectedBaseSideSettlementMethod(model.baseSettlementMethods.get(0));
            }
            if (!model.quoteSettlementMethods.isEmpty()) {
                model.setSelectedQuoteSideSettlementMethod(model.quoteSettlementMethods.get(0));
            }

            updateStrings();
        }

        private void updateStrings() {
            Direction direction = model.getDirection();
            if (direction == null) return;

            Market market = model.getSelectedMarket();
            if (market == null) return;

            String baseSideVerb = direction == Direction.SELL ?
                    Res.offerbook.get("sending") :
                    Res.offerbook.get("receiving");
            String quoteSideVerb = direction == Direction.BUY ?
                    Res.offerbook.get("sending") :
                    Res.offerbook.get("receiving");
            model.baseSideDescription.set(Res.offerbook.get("createOffer.settlement.description",
                    baseSideVerb, market.baseCurrencyCode()));
            model.quoteSideDescription.set(Res.offerbook.get("createOffer.settlement.description",
                    quoteSideVerb, market.quoteCurrencyCode()));
        }

        public void onViewAttached() {
            model.selectedProtocolTypProperty().addListener(selectedProtocolListener);
            model.selectedMarketProperty().addListener(selectedMarketListener);
            model.directionProperty().addListener(directionListener);
        }

        public void onViewDetached() {
            model.selectedProtocolTypProperty().removeListener(selectedProtocolListener);
            model.selectedMarketProperty().removeListener(selectedMarketListener);
            model.directionProperty().removeListener(directionListener);
        }

        public void onSelectBaseSideSettlement(Settlement.Method value) {
            model.setSelectedBaseSideSettlementMethod(value);
        }

        public void onSelectQuoteSideSettlement(Settlement.Method value) {
            model.setSelectedQuoteSideSettlementMethod(value);
        }
    }

    @Getter
    public static class SettlementModel implements Model {
        @Delegate
        private final OfferPreparationModel offerPreparationModel;
        private final ObservableList<Settlement.Method> baseSettlementMethods = FXCollections.observableArrayList();
        private final ObservableList<Settlement.Method> quoteSettlementMethods = FXCollections.observableArrayList();
        private final StringProperty baseSideDescription = new SimpleStringProperty();
        private final StringProperty quoteSideDescription = new SimpleStringProperty();
        private final BooleanProperty baseSideVisibility = new SimpleBooleanProperty();
        private final BooleanProperty quoteSideVisibility = new SimpleBooleanProperty();

        public SettlementModel(OfferPreparationModel offerPreparationModel) {
            this.offerPreparationModel = offerPreparationModel;
        }
    }

    public static class SettlementView extends View<VBox, SettlementModel, SettlementController> {
        private final BisqComboBox<Settlement.Method> baseSideComboBox, quoteSideComboBox;
        private final ChangeListener<Settlement.Method> selectedBaseSideSettlementListener, selectedQuoteSideSettlementListener;
        private final BisqLabel baseSideLabel, quoteSideLabel;

        public SettlementView(SettlementModel model,
                              SettlementController controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            baseSideLabel = new BisqLabel();
            baseSideLabel.getStyleClass().add("titled-group-bg-label-active");
            baseSideComboBox = getComboBox(model.baseSettlementMethods);
            VBox.setMargin(baseSideComboBox, new Insets(0, 0, 20, 0));

            quoteSideLabel = new BisqLabel();
            quoteSideLabel.getStyleClass().add("titled-group-bg-label-active");
            quoteSideComboBox = getComboBox(model.quoteSettlementMethods);

            root.getChildren().addAll(baseSideLabel, baseSideComboBox, quoteSideLabel, quoteSideComboBox);

            // Listeners on model change
            selectedBaseSideSettlementListener = (o, old, newValue) -> baseSideComboBox.getSelectionModel().select(newValue);
            selectedQuoteSideSettlementListener = (o, old, newValue) -> quoteSideComboBox.getSelectionModel().select(newValue);
        }

        private BisqComboBox<Settlement.Method> getComboBox(ObservableList<Settlement.Method> settlementMethods) {
            BisqComboBox<Settlement.Method> comboBox = new BisqComboBox<>();
            comboBox.setItems(settlementMethods);
            comboBox.setMinWidth(300);
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable Settlement.Method value) {
                    return value != null ? Res.offerbook.get(value.toString()) : "";
                }

                @Override
                public Settlement.Method fromString(String value) {
                    return null;
                }
            });
            return comboBox;
        }

        public void onViewAttached() {
            baseSideComboBox.setOnAction(e -> controller.onSelectBaseSideSettlement(baseSideComboBox.getSelectionModel().getSelectedItem()));
            quoteSideComboBox.setOnAction(e -> controller.onSelectQuoteSideSettlement(quoteSideComboBox.getSelectionModel().getSelectedItem()));
            model.selectedBaseSideSettlementMethodProperty().addListener(selectedBaseSideSettlementListener);
            model.selectedQuoteSideSettlementMethodProperty().addListener(selectedQuoteSideSettlementListener);

            baseSideLabel.textProperty().bind(model.baseSideDescription);

            baseSideLabel.visibleProperty().bind(model.baseSideVisibility);
            baseSideLabel.managedProperty().bind(model.baseSideVisibility);
            baseSideComboBox.visibleProperty().bind(model.baseSideVisibility);
            baseSideComboBox.managedProperty().bind(model.baseSideVisibility);

            quoteSideLabel.textProperty().bind(model.quoteSideDescription);

            quoteSideLabel.visibleProperty().bind(model.quoteSideVisibility);
            quoteSideLabel.managedProperty().bind(model.quoteSideVisibility);
            quoteSideComboBox.visibleProperty().bind(model.quoteSideVisibility);
            quoteSideComboBox.managedProperty().bind(model.quoteSideVisibility);
        }

        public void onViewDetached() {
            baseSideComboBox.setOnAction(null);
            quoteSideComboBox.setOnAction(null);
            model.selectedBaseSideSettlementMethodProperty().removeListener(selectedBaseSideSettlementListener);
            model.selectedQuoteSideSettlementMethodProperty().removeListener(selectedQuoteSideSettlementListener);

            baseSideLabel.textProperty().unbind();

            baseSideLabel.visibleProperty().unbind();
            baseSideLabel.managedProperty().unbind();
            baseSideComboBox.visibleProperty().unbind();
            baseSideComboBox.managedProperty().unbind();

            quoteSideLabel.textProperty().unbind();

            quoteSideLabel.visibleProperty().unbind();
            quoteSideLabel.managedProperty().unbind();
            quoteSideComboBox.visibleProperty().unbind();
            quoteSideComboBox.managedProperty().unbind();
        }
    }
}