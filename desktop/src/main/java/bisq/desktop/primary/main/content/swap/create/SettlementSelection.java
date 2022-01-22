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
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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

        public SettlementController(ObservableList<Settlement.Method> askSettlementMethods,
                                    ObservableList<Settlement.Method> bidSettlementMethods,
                                    ObjectProperty<Settlement.Method> askSelectedSettlement,
                                    ObjectProperty<Settlement.Method> bidSelectedSettlement) {
            this.model = new SettlementModel(askSettlementMethods,
                    bidSettlementMethods,
                    askSelectedSettlement,
                    bidSelectedSettlement);
            view = new SettlementView(model, this);
        }

        public void onSelectAsk(Settlement.Method value) {
            model.askSelectedSettlement.set(value);
        }

        public void onSelectBid(Settlement.Method value) {
            model.bidSelectedSettlement.set(value);
        }
    }

    @Getter
    public static class SettlementModel implements Model {
        private final ObservableList<Settlement.Method> askSettlementMethods;
        private final ObservableList<Settlement.Method> bidSettlementMethods;
        private final ObjectProperty<Settlement.Method> askSelectedSettlement;
        private final ObjectProperty<Settlement.Method> bidSelectedSettlement;
        public boolean hasFocus;

        public SettlementModel(ObservableList<Settlement.Method> askSettlementMethods,
                               ObservableList<Settlement.Method> bidSettlementMethods,
                               ObjectProperty<Settlement.Method> askSelectedSettlement,
                               ObjectProperty<Settlement.Method> bidSelectedSettlement) {
            this.askSettlementMethods = askSettlementMethods;
            this.bidSettlementMethods = bidSettlementMethods;
            this.askSelectedSettlement = askSelectedSettlement;
            this.bidSelectedSettlement = bidSelectedSettlement;
        }
    }

    public static class SettlementView extends View<VBox, SettlementModel, SettlementController> {
        private final BisqComboBox<Settlement.Method> ask, bid;
        private final ChangeListener<Settlement.Method> askSelectedSettlementListener, bidSelectedSettlementListener;

        public SettlementView(SettlementModel model,
                              SettlementController controller) {
            super(new VBox(), model, controller);

            ask = getComboBox(model.askSettlementMethods);
            bid = getComboBox(model.bidSettlementMethods);

            root.setPadding(new Insets(10, 0, 0, 0));
            root.setSpacing(2);
            root.getChildren().addAll(ask);

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
            model.askSelectedSettlement.addListener(askSelectedSettlementListener);
            model.bidSelectedSettlement.addListener(bidSelectedSettlementListener);
        }

        public void onViewDetached() {
            ask.setOnAction(null);
            bid.setOnAction(null);
            model.askSelectedSettlement.removeListener(askSelectedSettlementListener);
            model.bidSelectedSettlement.removeListener(bidSelectedSettlementListener);
        }
    }
}