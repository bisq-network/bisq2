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

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletReceiveController implements Controller {
    @Getter
    private final WalletReceiveView view;
    private final WalletReceiveModel model;
    private final ElectrumWalletService electrumWalletService;

    public WalletReceiveController(DefaultApplicationService applicationService) {
        electrumWalletService = applicationService.getElectrumWalletService();
        model = new WalletReceiveModel();
        view = new WalletReceiveView(model, this);
    }

    @Override
    public void onActivate() {
        electrumWalletService.getNewAddress().
                thenAccept(receiveAddress -> UIThread.run(() -> model.getReceiveAddress().setValue(receiveAddress)));
    }

    @Override
    public void onDeactivate() {
    }

    void onCopyToClipboard() {
        ClipboardUtil.copyToClipboard(model.getReceiveAddress().get());
    }
}
