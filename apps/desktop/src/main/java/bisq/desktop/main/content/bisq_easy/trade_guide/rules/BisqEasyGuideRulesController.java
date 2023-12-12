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

package bisq.desktop.main.content.bisq_easy.trade_guide.rules;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyGuideRulesController implements Controller {
    private final BisqEasyGuideRulesModel model;
    @Getter
    private final BisqEasyGuideRulesView view;
    private final SettingsService settingsService;

    public BisqEasyGuideRulesController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new BisqEasyGuideRulesModel();
        view = new BisqEasyGuideRulesView(model, this);
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
        Browser.open("https://bisq.wiki/Bisq_Easy");
    }

    void onConfirm(boolean selected) {
        settingsService.setTradeRulesConfirmed(selected);
        model.getTradeRulesConfirmed().set(selected);
       /* if (selected) {
            OverlayController.hide();
        }*/
    }

    void onClose() {
        OverlayController.hide();
    }
}
