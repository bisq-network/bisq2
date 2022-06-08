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

package bisq.desktop.primary.overlay.onboarding.bisqeasy.old.tab2;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import lombok.Getter;

public class BisqEasyOnboardingTab2Controller implements Controller {

    private final BisqEasyOnboardingTab2Model model;
    @Getter
    private final BisqEasyOnboardingTab2View view;

    public BisqEasyOnboardingTab2Controller(DefaultApplicationService applicationService) {
        model = new BisqEasyOnboardingTab2Model(applicationService);
        view = new BisqEasyOnboardingTab2View(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_ONBOARDING_TAB3);
    }

    void onSkip() {
        Navigation.navigateTo(NavigationTarget.CREATE_PROFILE);
    }
}
