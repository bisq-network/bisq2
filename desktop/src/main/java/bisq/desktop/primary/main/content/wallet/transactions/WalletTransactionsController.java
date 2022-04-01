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

package bisq.desktop.primary.main.content.wallet.transactions;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.NonCachingController;
import bisq.wallets.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletTransactionsController implements NonCachingController {
    private final WalletService walletService;
    private final WalletTransactionsModel model;
    @Getter
    private final WalletTransactionsView view;

    public WalletTransactionsController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService();
        model = new WalletTransactionsModel();
        view = new WalletTransactionsView(model, this);
    }

    @Override
    public void onActivate() {
        walletService.listTransactions()
                .thenAccept(txs -> UIThread.run(() -> model.addTransactions(txs)));
    }

    @Override
    public void onDeactivate() {
    }
}
