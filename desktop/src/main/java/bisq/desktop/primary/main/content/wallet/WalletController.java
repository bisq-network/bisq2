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
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.wallet.dialog.WalletConfigDialogController;
import bisq.desktop.primary.main.content.wallet.receive.WalletReceiveController;
import bisq.desktop.primary.main.content.wallet.send.WalletSendController;
import bisq.desktop.primary.main.content.wallet.transactions.WalletTransactionsController;
import bisq.desktop.primary.main.content.wallet.utxos.WalletUtxosController;
import lombok.Getter;

import java.util.Optional;

public class WalletController extends TabController {
    @Getter
    private final WalletModel model;
    @Getter
    private final WalletView view;
    private final DefaultApplicationService applicationService;

    public WalletController(DefaultApplicationService applicationService) {
        super(NavigationTarget.WALLET);
        this.applicationService = applicationService;

        model = new WalletModel(applicationService);
        view = new WalletView(model, this);

        new WalletConfigDialogController(applicationService).showDialogAndConnectToWallet();
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget) {
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
}
