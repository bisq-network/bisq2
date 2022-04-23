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

package bisq.desktop.primary.main.content.wallet.utxos;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.wallets.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletUtxosController implements Controller {
    private final WalletService walletService;
    private final WalletUtxosModel model;
    @Getter
    private final WalletUtxosView view;

    public WalletUtxosController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        model = new WalletUtxosModel();
        view = new WalletUtxosView(model, this);
    }

    @Override
    public void onActivate() {
        walletService.listUnspent()
                .thenAccept(utxos -> UIThread.run(() -> model.addUtxos(utxos)));
    }

    @Override
    public void onDeactivate() {
    }
}
