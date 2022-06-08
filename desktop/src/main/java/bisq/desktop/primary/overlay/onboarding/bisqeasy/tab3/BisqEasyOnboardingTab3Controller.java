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

package bisq.desktop.primary.overlay.onboarding.bisqeasy.tab3;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import lombok.Getter;

public class BisqEasyOnboardingTab3Controller implements Controller {

    private final BisqEasyOnboardingTab3Model model;
    @Getter
    private final BisqEasyOnboardingTab3View view;

    public BisqEasyOnboardingTab3Controller(DefaultApplicationService applicationService) {
        model = new BisqEasyOnboardingTab3Model(applicationService);
        view = new BisqEasyOnboardingTab3View(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.ONBOARDING_CREATE_PROFILE);
    }

    void onSkip() {
        Navigation.navigateTo(NavigationTarget.ONBOARDING_CREATE_PROFILE);
    }
}
