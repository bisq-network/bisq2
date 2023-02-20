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

package bisq.desktop.primary.main.content.wallet.txs;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.wallets.core.WalletService;
import bisq.wallets.core.model.Transaction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletTxsController implements Controller {
    @Getter
    private final WalletTxsView view;
    private final WalletTxsModel model;
    private final WalletService walletService;
    private Pin transactionsPin;

    public WalletTxsController(DefaultApplicationService applicationService) {
        walletService = applicationService.getWalletService().orElseThrow();
        model = new WalletTxsModel();
        view = new WalletTxsView(model, this);
    }

    @Override
    public void onActivate() {
        transactionsPin = FxBindings.<Transaction, WalletTransactionListItem>bind(model.getListItems())
                .map(WalletTransactionListItem::new)
                .to(walletService.getTransactions());

        walletService.requestTransactions();
    }

    @Override
    public void onDeactivate() {
        transactionsPin.unbind();
    }
}
