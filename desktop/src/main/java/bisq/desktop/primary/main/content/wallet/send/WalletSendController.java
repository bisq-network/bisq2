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

import bisq.desktop.common.view.Controller;
import bisq.wallets.core.WalletService;
import lombok.Getter;

import java.util.Optional;

public class WalletSendController implements Controller {
    private final WalletService walletService;
    private final WalletSendModel model;
    @Getter
    private final WalletSendView view;

    public WalletSendController(WalletService walletService) {
        this.walletService = walletService;
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
        String passphraseString = model.getPassphrase();
        Optional<String> passphrase = Optional.ofNullable(passphraseString);
        walletService.sendToAddress(passphrase, model.getAddress(), Double.parseDouble(model.getAmount()));
    }
}
