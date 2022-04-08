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
import bisq.desktop.common.view.FxNavigationTargetTab;
import bisq.desktop.common.view.FxTabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletView extends FxTabView<JFXTabPane, WalletModel, WalletController> {

    public WalletView(WalletModel model, WalletController controller) {
        super(new JFXTabPane(), model, controller);
    }

    @Override
    protected void createAndAddTabs() {
        FxNavigationTargetTab transactionsTab = createTab(Res.get("wallet.tab.transactions"), NavigationTarget.WALLET_TRANSACTIONS);
        FxNavigationTargetTab sendTab = createTab(Res.get("send"), NavigationTarget.WALLET_SEND);
        FxNavigationTargetTab receiveTab = createTab(Res.get("wallet.tab.receive"), NavigationTarget.WALLET_RECEIVE);
        FxNavigationTargetTab utxosTab = createTab(Res.get("wallet.tab.utxos"), NavigationTarget.WALLET_UTXOS);
        root.getTabs().setAll(transactionsTab, sendTab, receiveTab, utxosTab);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
