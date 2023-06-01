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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.rules;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyHelpRulesController implements Controller {
    private final BisqEasyHelpRulesModel model;
    @Getter
    private final BisqEasyHelpRulesView view;
    private final SettingsService settingsService;

    public BisqEasyHelpRulesController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        model = new BisqEasyHelpRulesModel();
        view = new BisqEasyHelpRulesView(model, this);
    }

    @Override
    public void onActivate() {
        model.getTradeRulesConfirmed().set(settingsService.getTradeRulesConfirmed().get());
    }

    @Override
    public void onDeactivate() {
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE_PROCESS);
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisqeasy");
    }

    void onConfirm() {
        settingsService.setTradeRulesConfirmed(true);
        model.getTradeRulesConfirmed().set(true);
        OverlayController.hide();
    }

    void onClose() {
        OverlayController.hide();
    }
}
