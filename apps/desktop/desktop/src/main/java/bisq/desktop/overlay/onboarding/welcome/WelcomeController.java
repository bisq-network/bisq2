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

package bisq.desktop.overlay.onboarding.welcome;

import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.DontShowAgainService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static bisq.settings.DontShowAgainKey.WELCOME;

@Slf4j
public class WelcomeController implements Controller {
    @Getter
    private final WelcomeView view;
    private final OverlayController overlayController;
    private final DontShowAgainService dontShowAgainService;

    public WelcomeController(ServiceProvider serviceProvider) {
        overlayController = OverlayController.getInstance();
        WelcomeModel model = new WelcomeModel();
        view = new WelcomeView(model, this);
        dontShowAgainService = serviceProvider.getDontShowAgainService();
    }

    @Override
    public void onActivate() {
        // If we support next on enter, the input handling at the profile view is broken.
        // Needs to manually click inside to have real input focus. Not clear why... for now lets
        // keep support for enter key disabled.
        overlayController.setEnterKeyHandler(null);
        overlayController.setUseEscapeKeyHandler(false);
    }

    @Override
    public void onDeactivate() {

    }

    void onNext() {
        dontShowAgainService.dontShowAgain(WELCOME);
        Navigation.navigateTo(NavigationTarget.ONBOARDING_GENERATE_NYM);
    }
}
