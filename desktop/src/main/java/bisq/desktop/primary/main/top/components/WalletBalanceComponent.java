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
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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
        public void onViewAttached() {
            balancePin = FxBindings.bind(model.balanceAsCoinProperty).to(walletService.getObservableBalanceAsCoin());
        }

        @Override
        public void onViewDetached() {
            balancePin.unbind();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Coin> balanceAsCoinProperty = new SimpleObjectProperty<>(Coin.of(0, "BTC"));
        private final ObservableValue<String> formattedBalanceProperty = Bindings.createStringBinding(
                () -> Res.get("wallet.balance.box",
                        AmountFormatter.formatAmountWithCode(balanceAsCoinProperty.get())),
                balanceAsCoinProperty);
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setAlignment(Pos.BASELINE_CENTER);

            var descriptionLabel = new Label();
            descriptionLabel.textProperty().bind(model.formattedBalanceProperty);
            root.getChildren().add(descriptionLabel);
        }
    }
}
