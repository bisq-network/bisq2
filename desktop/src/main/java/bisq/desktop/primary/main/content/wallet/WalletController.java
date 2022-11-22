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

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;

public class WalletController implements Controller {
    private final ElectrumWalletService walletService;
    private final WalletModel model = new WalletModel();
    @Getter
    private final WalletView view = new WalletView(model, this);
    private final WalletWithdrawFundsPopup walletWithdrawFundsPopup;

    private Pin balancePin;

    public WalletController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        walletWithdrawFundsPopup = new WalletWithdrawFundsPopup(walletService);
    }

    @Override
    public void onActivate() {
        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(walletService.getObservableBalanceAsCoin());

        walletService.listTransactions()
                .thenAccept(txs -> UIThread.run(() -> model.addTransactions(txs)));

        walletService.getNewAddress().
                thenAccept(receiveAddress ->
                        UIThread.run(() -> model.getReceiveAddressProperty().setValue(receiveAddress))
                );
    }

    @Override
    public void onDeactivate() {
        balancePin.unbind();
    }

    public void onWithdrawButtonClicked() {
        walletWithdrawFundsPopup.show();
    }
}
