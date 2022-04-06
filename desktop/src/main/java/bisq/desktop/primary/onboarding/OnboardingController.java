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

package bisq.desktop.primary.onboarding;

import bisq.application.DefaultApplicationService;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.primary.onboarding.onboardNewbie.OnboardNewbieController;
import bisq.desktop.primary.onboarding.initUserProfile.InitUserProfileController;
import bisq.desktop.primary.onboarding.onboardProTrader.OnboardProTraderController;
import bisq.desktop.primary.onboarding.selectUserType.SelectUserTypeController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OnboardingController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final OnboardingModel model;
    @Getter
    private final OnboardingView view;

    public OnboardingController(DefaultApplicationService applicationService) {
        super(NavigationTarget.ONBOARDING);

        this.applicationService = applicationService;
        model = new OnboardingModel(applicationService.getUserProfileService());
        view = new OnboardingView(model, this);
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
            case INIT_USER_PROFILE -> {
                return Optional.of(new InitUserProfileController(applicationService));
            }
            case SELECT_USER_TYPE -> {
                return Optional.of(new SelectUserTypeController(applicationService));
            }
            case ONBOARD_NEWBIE -> {
                return Optional.of(new OnboardNewbieController(applicationService));
            }
            case ONBOARD_PRO_TRADER -> {
                return Optional.of(new OnboardProTraderController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
