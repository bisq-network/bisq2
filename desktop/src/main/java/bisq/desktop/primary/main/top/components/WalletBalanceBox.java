package bisq.desktop.primary.main.top.components;

import bisq.common.monetary.Coin;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.wallets.WalletService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class WalletBalanceBox {
    public static class WalletBalanceController implements Controller {
        private final WalletBalanceModel model;
        @Getter
        private final WalletBalanceView view;
        private final WalletService walletService;
        private Pin balancePin;

        public WalletBalanceController(WalletService walletService) {
            this.walletService = walletService;
            model = new WalletBalanceModel();
            view = new WalletBalanceView(model, this);

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

    public static class WalletBalanceModel implements Model {
        private final ObjectProperty<Coin> balanceAsCoinProperty = new SimpleObjectProperty<>(Coin.of(0, "BTC"));
        private final ObservableValue<String> formattedBalanceProperty = Bindings.createStringBinding(
                () -> Res.get("wallet.balance.box",
                        AmountFormatter.formatAmountWithCode(balanceAsCoinProperty.get())));
    }

    public static class WalletBalanceView extends View<VBox, WalletBalanceModel, WalletBalanceController> {
        public WalletBalanceView(WalletBalanceModel model, WalletBalanceController controller) {
            super(new VBox(), model, controller);
            root.setAlignment(Pos.BASELINE_CENTER);

            var descriptionLabel = new Label();
            descriptionLabel.textProperty().bind(model.formattedBalanceProperty);
            root.getChildren().add(descriptionLabel);
        }
    }
}
