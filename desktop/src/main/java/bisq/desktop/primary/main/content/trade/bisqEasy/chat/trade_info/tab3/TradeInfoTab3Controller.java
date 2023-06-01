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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_info.tab3;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeInfoTab3Controller implements Controller {
    private final TradeInfoTab3Model model;
    @Getter
    private final TradeInfoTab3View view;
    private final SettingsService settingsService;
    private final Runnable collapseHandler;

    public TradeInfoTab3Controller(DefaultApplicationService applicationService, Runnable collapseHandler) {
        settingsService = applicationService.getSettingsService();
        this.collapseHandler = collapseHandler;
        model = new TradeInfoTab3Model();
        view = new TradeInfoTab3View(model, this);
    }

    @Override
    public void onActivate() {
        model.getTradeRulesConfirmed().set(settingsService.getTradeRulesConfirmed().get());
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.TRADE_INFO_TAB_2);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisqeasy");
    }

    void onConfirm(boolean value) {
        settingsService.setTradeRulesConfirmed(value);
        model.getTradeRulesConfirmed().set(value);
    }

    void onCollapse() {
        collapseHandler.run();
    }
}
