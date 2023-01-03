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

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;

public class WalletView extends TabView<WalletModel, WalletController> {

    public WalletView(WalletModel model, WalletController controller) {
        super(model, controller);

        addTab(Res.get("wallet.dashboard"), NavigationTarget.WALLET_DASHBOARD);
        addTab(Res.get("wallet.send"), NavigationTarget.WALLET_SEND);
        addTab(Res.get("wallet.receive"), NavigationTarget.WALLET_RECEIVE);
        addTab(Res.get("wallet.txs"), NavigationTarget.WALLET_TXS);
        addTab(Res.get("wallet.settings"), NavigationTarget.WALLET_SETTINGS);

        headLine.setText(Res.get("wallet"));
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
