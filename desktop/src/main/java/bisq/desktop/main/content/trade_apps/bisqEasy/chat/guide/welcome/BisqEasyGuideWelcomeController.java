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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.guide.welcome;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyGuideWelcomeController implements Controller {
    @Getter
    private final BisqEasyGuideWelcomeView view;
    private final SettingsService settingsService;
    private final BisqEasyGuideWelcomeModel model;

    public BisqEasyGuideWelcomeController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new BisqEasyGuideWelcomeModel();
        view = new BisqEasyGuideWelcomeView(model, this);
    }

    @Override
    public void onActivate() {
        String content = Res.get("tradeGuide.welcome.content");
        model.getContentText().setValue(content);
      /*  model.getContentText().setValue(settingsService.getTradeRulesConfirmed().get() ?
                content :
                content + "\n\n" + Res.get("tradeGuide.welcome.content.notYetConfirmed"));*/
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE_SECURITY);
    }
}
