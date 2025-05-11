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

package bisq.desktop.main.content.mu_sig.offerbook.sell;

import bisq.desktop.common.Styles;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigSellOfferbookTabView extends MuSigLevel2TabView<MuSigSellOfferbookTabModel, MuSigSellOfferbookTabController> {
    public MuSigSellOfferbookTabView(MuSigSellOfferbookTabModel model, MuSigSellOfferbookTabController controller) {
        super(model, controller);
    }

    @Override
    protected void addTabs() {
        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("muSig.offerbook.btc"),
                NavigationTarget.MU_SIG_OFFERBOOK_SELL_BTC,
                styles);
        addTab(Res.get("muSig.offerbook.xmr"),
                NavigationTarget.MU_SIG_OFFERBOOK_SELL_XMR,
                styles);
        addTab(Res.get("muSig.offerbook.other"),
                NavigationTarget.MU_SIG_OFFERBOOK_SELL_OTHER,
                styles);
    }
}
