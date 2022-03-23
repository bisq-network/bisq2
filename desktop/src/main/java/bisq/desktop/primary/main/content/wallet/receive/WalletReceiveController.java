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
import bisq.wallets.WalletService;
import lombok.Getter;

public class WalletReceiveController implements Controller {
    private final WalletService walletService;
    private final WalletReceiveModel model;
    @Getter
    private final WalletReceiveView view;

    public WalletReceiveController(DefaultApplicationService applicationService) {
        this.walletService = applicationService.getWalletService();
        model = new WalletReceiveModel();
        view = new WalletReceiveView(model, this);
    }

    public void onGenerateNewAddress() {
        walletService.getNewAddress("")
                .thenAccept(newAddress -> UIThread.run(() -> model.setAddress(newAddress)));
    }

    public void copyAddress() {
        ClipboardUtil.copyToClipboard(model.getAddress().getValue());
    }
}
