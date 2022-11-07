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

package bisq.desktop.primary.main.content.wallet.receive;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;

public class WalletReceiveController implements Controller {
    private final ElectrumWalletService walletService;
    private final WalletReceiveModel model;
    @Getter
    private final WalletReceiveView view;

    private Pin receiveAddressListPin;

    public WalletReceiveController(ElectrumWalletService walletService) {
        this.walletService = walletService;
        model = new WalletReceiveModel();
        view = new WalletReceiveView(model, this);
    }

    @Override
    public void onActivate() {
        receiveAddressListPin = FxBindings.<String, String>bind(model.getListItems())
                .to(walletService.getReceiveAddresses());
    }

    @Override
    public void onDeactivate() {
        receiveAddressListPin.unbind();
    }

    public void onGenerateNewAddress() {
        walletService.getNewAddress();
    }
}
