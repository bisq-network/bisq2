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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.desktop.navigation.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.wallets.core.WalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletDashboardController implements Controller {
    @Getter
    private final WalletDashboardView view;
    private final WalletDashboardModel model;
    private final WalletService walletService;
    private Pin balancePin;

    public WalletDashboardController(ServiceProvider serviceProvider) {
        walletService = serviceProvider.getWalletService().orElseThrow();
        model = new WalletDashboardModel();
        view = new WalletDashboardView(model, this);
    }

    @Override
    public void onActivate() {
        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(walletService.getBalance());

        walletService.requestBalance().whenComplete((balance, throwable) -> {
            if (throwable == null) {
                UIThread.run(() -> model.getBalanceAsCoinProperty().set(balance));
            }
        });
    }

    @Override
    public void onDeactivate() {
        balancePin.unbind();
    }

    void onSend() {
        Navigation.navigateTo(NavigationTarget.WALLET_SEND);
    }

    void onReceive() {
        Navigation.navigateTo(NavigationTarget.WALLET_RECEIVE);
    }
}
