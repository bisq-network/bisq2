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

package bisq.desktop.primary.main.content.wallet;

import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.wallets.electrum.ElectrumWalletService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class WalletWithdrawFundsPopup extends Popup {
    private final Controller controller;

    public WalletWithdrawFundsPopup(ElectrumWalletService walletService) {
        super();
        controller = new Controller(walletService, this);
    }

    @Override
    public void addContent() {
        super.addContent();
        controller.addContent();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ElectrumWalletService walletService;

        private final Model model;
        @Getter
        private final View view;

        private Controller(ElectrumWalletService walletService, Popup popup) {
            this.walletService = walletService;

            model = new Model();
            view = new View(model, this, popup);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        public void onWithdraw() {
            String passphraseString = model.passphraseProperty.get();
            Optional<String> passphrase = Optional.ofNullable(passphraseString);
            walletService.sendToAddress(passphrase, model.addressProperty.get(),
                            Double.parseDouble(model.amountProperty.get()))
                    .whenComplete((response, throwable) -> {
                        if (throwable == null) {
                            log.error(response);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        }

        private void addContent() {
            view.addContent();
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty addressProperty = new SimpleStringProperty(this, "address");
        private final StringProperty amountProperty = new SimpleStringProperty(this, "amount");
        private final StringProperty passphraseProperty = new SimpleStringProperty(this, "passphrase");
    }

    private static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final Popup popup;

        private View(Model model, Controller controller, Popup popup) {
            super(new Pane(), model, controller);
            this.popup = popup;

            popup.headLine(Res.get("wallet.withdrawFromWallet"));
            popup.message(Res.get("wallet.withdrawToExternalWalletMessage"));
            popup.actionButtonText(Res.get("wallet.withdraw"));
            popup.onAction(controller::onWithdraw);
            popup.doCloseOnAction(true);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }

        private void addContent() {
            BisqGridPane gridPane = popup.getGridPane();
            gridPane.addTextField(Res.get("address"), model.addressProperty);
            gridPane.addTextField(Res.get("amount"), model.amountProperty);
            gridPane.addPasswordField(Res.get("passphrase"), model.passphraseProperty);
        }
    }
}
