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

package bisq.desktop.main.content.bisq_easy.onboarding;

import bisq.desktop.navigation.NavigationTarget;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyOnboardingController implements Controller {
    @Getter
    private final BisqEasyOnboardingView view;
    private final BisqEasyOnboardingModel model;
    private final SettingsService settingsService;
    private Pin cookieChangedPin;

    public BisqEasyOnboardingController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        model = new BisqEasyOnboardingModel();
        view = new BisqEasyOnboardingView(model, this);
    }

    @Override
    public void onActivate() {
        cookieChangedPin = settingsService.getCookieChanged().addObserver(c -> UIThread.run(() ->
                model.getVideoSeen().set(settingsService.getCookie().asBoolean(CookieKey.BISQ_EASY_VIDEO_OPENED).orElse(false))));
    }

    @Override
    public void onDeactivate() {
        cookieChangedPin.unbind();
    }

    void onOpenOfferbook() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_OFFERBOOK);
    }

    void onOpenTradeWizard() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_TRADE_WIZARD, new TradeWizardController.InitData(false));
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }

    void onPlayVideo() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_VIDEO);
    }
}
