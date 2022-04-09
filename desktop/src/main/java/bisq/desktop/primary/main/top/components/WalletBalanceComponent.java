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

import bisq.common.monetary.Coin;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.wallets.WalletService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;

public class WalletBalanceComponent {
    private final Controller controller;

    public WalletBalanceComponent(WalletService walletService) {
        controller = new Controller(walletService);
    }

    public Pane getRootPane() {
        return controller.getView().getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final WalletService walletService;
        private Pin balancePin;

        private Controller(WalletService walletService) {
            this.walletService = walletService;
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            balancePin = FxBindings.bind(model.balanceAsCoinProperty).to(walletService.getObservableBalanceAsCoin());
        }

        @Override
        public void onDeactivate() {
            balancePin.unbind();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Coin> balanceAsCoinProperty = new SimpleObjectProperty<>(Coin.of(0, "BTC"));
        private final ObservableValue<String> formattedBalanceProperty = Bindings.createStringBinding(
                () -> AmountFormatter.formatAmountWithCode(balanceAsCoinProperty.get()),
                balanceAsCoinProperty);
    }

    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label balance;

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);

           // root.setAlignment(Pos.BASELINE_CENTER);
            root.setSpacing(13);

            Label label = new Label(Res.get("wallet.availableBalance").toUpperCase());
            label.setStyle("-fx-text-fill: -bisq-text-medium; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.25em");
            label.setPadding(new Insets(4,0,0,0));
            
            balance = new Label();
            balance.setStyle("-fx-text-fill: -bisq-text; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.55em");
           
            root.getChildren().addAll(label,balance );
        }

        @Override
        protected void onViewAttached() {
            balance.textProperty().bind(model.formattedBalanceProperty);
        }

        @Override
        protected void onViewDetached() {
            balance.textProperty().unbind();
        }
    }
}
