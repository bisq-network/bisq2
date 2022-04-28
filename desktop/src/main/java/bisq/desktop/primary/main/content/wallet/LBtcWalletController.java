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
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.wallets.WalletService;

public class LBtcWalletController extends WalletController {

    public LBtcWalletController(DefaultApplicationService applicationService) {
        super(applicationService, NavigationTarget.WALLET_LBTC);
    }

    @Override
    public WalletService getWalletService() {
        return applicationService.getLBtcWalletService();
    }

    @Override
    public NavigationTarget getTransactionsTabNavigationTarget() {
        return NavigationTarget.WALLET_LBTC_TRANSACTIONS;
    }

    @Override
    public NavigationTarget getSendTabNavigationTarget() {
        return NavigationTarget.WALLET_LBTC_SEND;
    }

    @Override
    public NavigationTarget getReceiveTabNavigationTarget() {
        return NavigationTarget.WALLET_LBTC_RECEIVE;
    }

    @Override
    public NavigationTarget getUtxoTabNavigationTarget() {
        return NavigationTarget.WALLET_LBTC_UTXOS;
    }

    @Override
    protected void onConfigPopupClosed() {
        Navigation.navigateTo(NavigationTarget.WALLET_LBTC_TRANSACTIONS);
    }
}
