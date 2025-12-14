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

package bisq.desktop.main.content.mu_sig.trade_limits.tab1;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.navigation.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeLimitsTab1Controller implements Controller {
    @Getter
    private final TradeLimitsTab1View view;

    public TradeLimitsTab1Controller(ServiceProvider serviceProvider) {
        TradeLimitsTab1Model model = new TradeLimitsTab1Model();
        view = new TradeLimitsTab1View(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/Reputation");
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.MU_SIG_TRADE_LIMITS_TAB_2);
    }
}
