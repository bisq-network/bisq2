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

package bisq.desktop.main.content.mu_sig.market;

import bisq.desktop.common.Styles;
import bisq.desktop.main.content.mu_sig.offerbook.MuSigLevel2TabView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigMarketTabView extends MuSigLevel2TabView<MuSigMarketTabModel, MuSigMarketTabController> {
    public MuSigMarketTabView(MuSigMarketTabModel model, MuSigMarketTabController controller) {
        super(model, controller);
    }

    @Override
    protected void addTabs() {
        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("muSig.market.chart"),
                NavigationTarget.MU_SIG_MARKET_CHART,
                styles);
        addTab(Res.get("muSig.market.byCurrency"),
                NavigationTarget.MU_SIG_MARKET_BY_CURRENCY,
                styles);
        addTab(Res.get("muSig.market.byPaymentMethods"),
                NavigationTarget.MU_SIG_MARKET_BY_PAYMENT_METHODS,
                styles);
    }
}

