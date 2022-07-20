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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.help.tab3;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyHelpTab3Controller implements Controller {
    private final BisqEasyHelpTab3Model model;
    @Getter
    private final BisqEasyHelpTab3View view;
    private final SettingsService settingsService;

    public BisqEasyHelpTab3Controller(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        model = new BisqEasyHelpTab3Model();
        view = new BisqEasyHelpTab3View(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_HELP_TAB_2);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisqeasy");
    }
}
