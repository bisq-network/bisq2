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

package bisq.desktop.primary.main.content.wallet.dashboard;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.wallets.electrum.ElectrumWalletService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletDashboardController implements Controller {
    @Getter
    private final WalletDashboardView view;
    private final WalletDashboardModel model;
    private final ElectrumWalletService electrumWalletService;
    private Pin balancePin;

    public WalletDashboardController(DefaultApplicationService applicationService) {
        electrumWalletService = applicationService.getElectrumWalletService();
        model = new WalletDashboardModel();
        view = new WalletDashboardView(model, this);
    }

    @Override
    public void onActivate() {
        balancePin = FxBindings.bind(model.getBalanceAsCoinProperty())
                .to(electrumWalletService.getObservableBalanceAsCoin());
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
