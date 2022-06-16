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

package bisq.desktop.primary.overlay.onboarding.bisqeasy;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyIntroController implements Controller {
    private final BisqEasyIntroModel model;
    @Getter
    private final BisqEasyIntroView view;
    private final SettingsService settingsService;

    public BisqEasyIntroController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        model = new BisqEasyIntroModel();
        view = new BisqEasyIntroView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        settingsService.setCookie(CookieKey.BISQ_EASY_ONBOARDED, true);
        OverlayController.hide();
        Navigation.navigateTo(NavigationTarget.MAIN);
        UIThread.runOnNextRenderFrame(() -> Navigation.navigateTo(NavigationTarget.DASHBOARD));
    }

    public void onSkip() {
        settingsService.setCookie(CookieKey.BISQ_EASY_ONBOARDED, true);
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER);
    }
}
