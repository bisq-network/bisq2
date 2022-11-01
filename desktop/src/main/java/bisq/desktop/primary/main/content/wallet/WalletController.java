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
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.wallet.receive.WalletReceiveController;
import bisq.desktop.primary.main.content.wallet.send.WalletSendController;
import bisq.desktop.primary.main.content.wallet.transactions.WalletTransactionsController;
import bisq.desktop.primary.main.content.wallet.utxos.WalletUtxosController;
import bisq.wallets.core.WalletService;
import lombok.Getter;

import java.util.Optional;

public abstract class WalletController extends TabController<WalletModel> implements Controller {
    @Getter
    private final WalletView view;
    protected final DefaultApplicationService applicationService;
    protected final WalletService walletService;

    public WalletController(DefaultApplicationService applicationService, NavigationTarget navigationTarget) {
        super(new WalletModel(), navigationTarget);

        this.applicationService = applicationService;
        walletService = getWalletService();

        view = new WalletView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget) {
        if (!isWalletReady()) {
            return Optional.empty();
        }

        if (navigationTarget == getTransactionsTabNavigationTarget()) {
            return Optional.of(new WalletTransactionsController(walletService));

        } else if (navigationTarget == getSendTabNavigationTarget()) {
            return Optional.of(new WalletSendController(walletService));

        } else if (navigationTarget == getReceiveTabNavigationTarget()) {
            return Optional.of(new WalletReceiveController(walletService));

        } else if (navigationTarget == getUtxoTabNavigationTarget()) {
            return Optional.of(new WalletUtxosController(walletService));

        } else {
            return Optional.empty();
        }
    }

    public abstract WalletService getWalletService();

    public abstract NavigationTarget getTransactionsTabNavigationTarget();

    public abstract NavigationTarget getSendTabNavigationTarget();

    public abstract NavigationTarget getReceiveTabNavigationTarget();

    public abstract NavigationTarget getUtxoTabNavigationTarget();

    protected boolean isWalletReady() {
        return walletService.isWalletReady();
    }
}
