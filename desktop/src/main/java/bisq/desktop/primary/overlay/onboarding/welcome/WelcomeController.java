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

package bisq.desktop.primary.overlay.onboarding.welcome;

import bisq.desktop.DesktopApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.settings.DontShowAgainService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static bisq.settings.DontShowAgainKey.WELCOME;

@Slf4j
public class WelcomeController implements Controller {
    @Getter
    private final WelcomeView view;

    public WelcomeController(DesktopApplicationService applicationService) {
        WelcomeModel model = new WelcomeModel();
        view = new WelcomeView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        DontShowAgainService.dontShowAgain(WELCOME);
        Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
    }
}
