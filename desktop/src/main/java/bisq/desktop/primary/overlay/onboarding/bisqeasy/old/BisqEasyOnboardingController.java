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

package bisq.desktop.primary.overlay.onboarding.bisqeasy.old;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.overlay.onboarding.bisqeasy.old.tab1.BisqEasyOnboardingTab1Controller;
import bisq.desktop.primary.overlay.onboarding.bisqeasy.old.tab2.BisqEasyOnboardingTab2Controller;
import bisq.desktop.primary.overlay.onboarding.bisqeasy.old.tab3.BisqEasyOnboardingTab3Controller;
import lombok.Getter;

import java.util.Optional;

public class BisqEasyOnboardingController extends TabController<BisqEasyOnboardingModel> {
    @Getter
    private final BisqEasyOnboardingView view;
    private final DefaultApplicationService applicationService;

    public BisqEasyOnboardingController(DefaultApplicationService applicationService) {
        super(new BisqEasyOnboardingModel(), NavigationTarget.ONBOARDING_BISQ_EASY);
       
        this.applicationService = applicationService;
        view = new BisqEasyOnboardingView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case BISQ_EASY_ONBOARDING_TAB1 -> {
                return Optional.of(new BisqEasyOnboardingTab1Controller(applicationService));
            }
            case BISQ_EASY_ONBOARDING_TAB2 -> {
                return Optional.of(new BisqEasyOnboardingTab2Controller(applicationService));
            }
            case BISQ_EASY_ONBOARDING_TAB3 -> {
                return Optional.of(new BisqEasyOnboardingTab3Controller(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
