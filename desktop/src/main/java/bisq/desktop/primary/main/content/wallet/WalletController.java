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
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.wallet.config.WalletConfigPopup;
import bisq.desktop.primary.main.content.wallet.receive.WalletReceiveController;
import bisq.desktop.primary.main.content.wallet.send.WalletSendController;
import bisq.desktop.primary.main.content.wallet.transactions.WalletTransactionsController;
import bisq.desktop.primary.main.content.wallet.utxos.WalletUtxosController;
import bisq.wallets.WalletService;
import lombok.Getter;

import java.util.Optional;

public class WalletController extends TabController {
    @Getter
    private final WalletModel model;
    @Getter
    private final WalletView view;
    private final DefaultApplicationService applicationService;
    private final WalletConfigPopup walletConfigPopup;
    private final WalletService walletService;

    public WalletController(DefaultApplicationService applicationService) {
        super(NavigationTarget.WALLET);
        this.applicationService = applicationService;
        walletService = applicationService.getWalletService();

        model = new WalletModel();
        view = new WalletView(model, this);

        walletConfigPopup = new WalletConfigPopup(applicationService, this::onConfigPopupClosed);
    }

    @Override
    public void onActivate() {
        if (!isWalletReady()) {
            walletConfigPopup.show();
        }
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget) {
        if (!isWalletReady()) {
            return Optional.empty();
        }

        switch (navigationTarget) {
            case WALLET_TRANSACTIONS -> {
                return Optional.of(new WalletTransactionsController(applicationService));
            }
            case WALLET_SEND -> {
                return Optional.of(new WalletSendController(applicationService));
            }
            case WALLET_RECEIVE -> {
                return Optional.of(new WalletReceiveController(applicationService));
            }
            case WALLET_UTXOS -> {
                return Optional.of(new WalletUtxosController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private boolean isWalletReady() {
        return walletService.isWalletReady();
    }

    private void onConfigPopupClosed() {
        Navigation.navigateTo(model.getDefaultNavigationTarget());
    }
}
