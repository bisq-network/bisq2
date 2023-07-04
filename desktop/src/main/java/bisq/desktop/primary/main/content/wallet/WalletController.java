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

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.wallet.dashboard.WalletDashboardController;
import bisq.desktop.primary.main.content.wallet.receive.WalletReceiveController;
import bisq.desktop.primary.main.content.wallet.send.WalletSendController;
import bisq.desktop.primary.main.content.wallet.settings.WalletSettingsController;
import bisq.desktop.primary.main.content.wallet.txs.WalletTxsController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class WalletController extends TabController<WalletModel> {
    private final ServiceProvider serviceProvider;
    @Getter
    private final WalletView view;

    public WalletController(ServiceProvider serviceProvider) {
        super(new WalletModel(), NavigationTarget.WALLET);

        this.serviceProvider = serviceProvider;

        view = new WalletView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case WALLET_DASHBOARD: {
                return Optional.of(new WalletDashboardController(serviceProvider));
            }
            case WALLET_SEND: {
                return Optional.of(new WalletSendController(serviceProvider));
            }
            case WALLET_RECEIVE: {
                return Optional.of(new WalletReceiveController(serviceProvider));
            }
            case WALLET_TXS: {
                return Optional.of(new WalletTxsController(serviceProvider));
            }
            case WALLET_SETTINGS: {
                return Optional.of(new WalletSettingsController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }
}
