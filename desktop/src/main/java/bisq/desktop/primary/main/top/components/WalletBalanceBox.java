package bisq.desktop.primary.main.top.components;

import bisq.common.locale.LocaleRepository;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import bisq.wallets.WalletService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class WalletBalanceBox {
    public static class WalletBalanceController implements Controller, WalletService.BalanceListener {
        private final WalletBalanceModel model;
        @Getter
        private final WalletBalanceView view;

        public WalletBalanceController(WalletService walletService) {
            model = new WalletBalanceModel();
            view = new WalletBalanceView(model, this);
            walletService.addBalanceListener(this);
        }

        @Override
        public void onBalanceChanged(double newBalance) {
            UIThread.run(() -> model.balanceDoubleProperty.set(newBalance));
        }
    }

    public static class WalletBalanceModel implements Model {
        private final DoubleProperty balanceDoubleProperty = new SimpleDoubleProperty(this, "balance", 0);
        private final ObservableValue<String> formattedBalanceProperty = Bindings
                .format(
                        LocaleRepository.getDefaultLocale(),
                        Res.get("wallet.balance.box"),
                        balanceDoubleProperty
                );
    }

    public static class WalletBalanceView extends View<VBox, WalletBalanceModel, WalletBalanceController> {
        public WalletBalanceView(WalletBalanceModel model, WalletBalanceController controller) {
            super(new VBox(), model, controller);
            root.setAlignment(Pos.BASELINE_CENTER);

            var descriptionLabel = new Label();
            descriptionLabel.textProperty().bind(model.formattedBalanceProperty);
            root.getChildren().addAll(descriptionLabel);
        }
    }
}
