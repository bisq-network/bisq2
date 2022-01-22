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

import bisq.account.settlement.Settlement;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.offer.protocol.ProtocolSpecifics;
import bisq.offer.protocol.SwapProtocolType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
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

        public SettlementController(ObjectProperty<Market> selectedMarket,
                                    ObjectProperty<SwapProtocolType> selectedProtocol,
                                    ObservableList<Settlement.Method> askSettlementMethods,
                                    ObservableList<Settlement.Method> bidSettlementMethods,
                                    ObjectProperty<Settlement.Method> askSelectedSettlement,
                                    ObjectProperty<Settlement.Method> bidSelectedSettlement) {
            this.model = new SettlementModel(selectedMarket,
                    selectedProtocol,
                    askSettlementMethods,
                    bidSettlementMethods,
                    askSelectedSettlement,
                    bidSelectedSettlement);
            view = new SettlementView(model, this);

            selectedProtocolListener = new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends SwapProtocolType> observable, SwapProtocolType oldValue, SwapProtocolType newValue) {
                    if (model.selectedMarket.get() == null) return;
                    model.baseSettlementMethods.setAll(ProtocolSpecifics.getSettlementMethods(newValue, model.selectedMarket.get().baseCurrencyCode()));
                }
            };
        }

        public void onViewAttached() {
            model.selectedProtocol.addListener(selectedProtocolListener);
        }

        public void onViewDetached() {
            model.selectedProtocol.removeListener(selectedProtocolListener);
        }

        public void onSelectAsk(Settlement.Method value) {
            model.baseSelectedSettlement.set(value);
        }

        public void onSelectBid(Settlement.Method value) {
            model.quoteSelectedSettlement.set(value);
        }
    }

    @Getter
    public static class SettlementModel implements Model {
        private final ObjectProperty<Market> selectedMarket;
        private final ObjectProperty<SwapProtocolType> selectedProtocol;
        private final ObservableList<Settlement.Method> baseSettlementMethods;
        private final ObservableList<Settlement.Method> quoteSettlementMethods;
        private final ObjectProperty<Settlement.Method> baseSelectedSettlement;
        private final ObjectProperty<Settlement.Method> quoteSelectedSettlement;

        public SettlementModel(ObjectProperty<Market> selectedMarket,
                               ObjectProperty<SwapProtocolType> selectedProtocol,
                               ObservableList<Settlement.Method> baseSettlementMethods,
                               ObservableList<Settlement.Method> quoteSettlementMethods,
                               ObjectProperty<Settlement.Method> baseSelectedSettlement,
                               ObjectProperty<Settlement.Method> quoteSelectedSettlement) {
            this.selectedMarket = selectedMarket;
            this.selectedProtocol = selectedProtocol;
            this.baseSettlementMethods = baseSettlementMethods;
            this.quoteSettlementMethods = quoteSettlementMethods;
            this.baseSelectedSettlement = baseSelectedSettlement;
            this.quoteSelectedSettlement = quoteSelectedSettlement;
        }
    }

    public static class SettlementView extends View<VBox, SettlementModel, SettlementController> {
        private final BisqComboBox<Settlement.Method> ask, bid;
        private final ChangeListener<Settlement.Method> askSelectedSettlementListener, bidSelectedSettlementListener;

        public SettlementView(SettlementModel model,
                              SettlementController controller) {
            super(new VBox(), model, controller);

            Label headline = new BisqLabel(Res.offerbook.get("createOffer.selectSettlement"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            ask = getComboBox(model.baseSettlementMethods);
            bid = getComboBox(model.quoteSettlementMethods);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(headline, ask);

            // Listeners on model change
            askSelectedSettlementListener = (o, old, newValue) -> ask.getSelectionModel().select(newValue);
            bidSelectedSettlementListener = (o, old, newValue) -> bid.getSelectionModel().select(newValue);
        }

        private BisqComboBox<Settlement.Method> getComboBox(ObservableList<Settlement.Method> settlementMethods) {
            BisqComboBox<Settlement.Method> comboBox = new BisqComboBox<>();
            comboBox.setMinHeight(42);
            comboBox.setItems(settlementMethods);
            comboBox.setMaxWidth(200);
            comboBox.setVisibleRowCount(10);
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable Settlement.Method value) {
                    //   return value != null ? Res.offerbook.get(value.toString()) : "";
                    return value != null ? value.toString() : "";
                }

                @Override
                public Settlement.Method fromString(String value) {
                    return null;
                }
            });
            return comboBox;
        }

        public void onViewAttached() {
            ask.setOnAction(e -> controller.onSelectAsk(ask.getSelectionModel().getSelectedItem()));
            bid.setOnAction(e -> controller.onSelectBid(bid.getSelectionModel().getSelectedItem()));
            model.baseSelectedSettlement.addListener(askSelectedSettlementListener);
            model.quoteSelectedSettlement.addListener(bidSelectedSettlementListener);
        }

        public void onViewDetached() {
            ask.setOnAction(null);
            bid.setOnAction(null);
            model.baseSelectedSettlement.removeListener(askSelectedSettlementListener);
            model.quoteSelectedSettlement.removeListener(bidSelectedSettlementListener);
        }
    }
}