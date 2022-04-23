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
import bisq.desktop.common.view.CachingController;
import bisq.wallets.WalletService;
import lombok.Getter;

public class WalletSendController implements CachingController {
    private final WalletService walletService;
    private final WalletSendModel model;
    @Getter
    private final WalletSendView view;

    public WalletSendController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        model = new WalletSendModel();
        view = new WalletSendView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onSendButtonClicked() {
        walletService.sendToAddress(model.getAddress(), Double.parseDouble(model.getAmount()));
    }
}
