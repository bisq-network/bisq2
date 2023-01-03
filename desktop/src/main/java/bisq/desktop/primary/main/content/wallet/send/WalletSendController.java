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

package bisq.desktop.primary.main.content.wallet.send;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class WalletSendController implements Controller {
    @Getter
    private final WalletSendView view;
    private final WalletSendModel model;
    private final ElectrumWalletService electrumWalletService;
    private final MonetaryValidator amountValidator = new MonetaryValidator();
    private Subscription addressPin;
    private Subscription amountPin;

    public WalletSendController(DefaultApplicationService applicationService) {
        electrumWalletService = applicationService.getElectrumWalletService();
        model = new WalletSendModel();
        view = new WalletSendView(model, this, amountValidator);
    }

    @Override
    public void onActivate() {
        addressPin = EasyBind.subscribe(model.getAddress(), customMethod -> {

        });
        amountPin = EasyBind.subscribe(model.getAmount(), customMethod -> {

        });

        //todo check if wallet is encrypted
        electrumWalletService.isWalletEncrypted()
                .thenAccept(isWalletEncrypted -> UIThread.run(() -> model.getIsPasswordVisible().set(isWalletEncrypted)));
    }

    @Override
    public void onDeactivate() {
        addressPin.unsubscribe();
        amountPin.unsubscribe();
    }

    void onSend() {
        //todo
        double amount = Double.parseDouble(model.getAmount().get());
        String address = model.getAddress().get();
        electrumWalletService.sendToAddress(Optional.ofNullable(model.getPassword().get()), address, amount)
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        UIThread.run(() -> new Popup().error(throwable.getMessage()).show());
                    }
                });
    }
}
