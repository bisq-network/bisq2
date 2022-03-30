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

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.NavigationTargetTab;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletView extends TabView<JFXTabPane, WalletModel, WalletController> {

    public WalletView(WalletModel model, WalletController controller) {
        super(new JFXTabPane(), model, controller);
    }

    @Override
    protected void createAndAddTabs() {
        NavigationTargetTab transactionsTab = createTab(Res.get("wallet.tab.transactions"), NavigationTarget.WALLET_TRANSACTIONS);
        NavigationTargetTab sendTab = createTab(Res.get("send"), NavigationTarget.WALLET_SEND);
        NavigationTargetTab receiveTab = createTab(Res.get("wallet.tab.receive"), NavigationTarget.WALLET_RECEIVE);
        NavigationTargetTab utxosTab = createTab(Res.get("wallet.tab.utxos"), NavigationTarget.WALLET_UTXOS);
        root.getTabs().setAll(transactionsTab, sendTab, receiveTab, utxosTab);
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();
    }
}
