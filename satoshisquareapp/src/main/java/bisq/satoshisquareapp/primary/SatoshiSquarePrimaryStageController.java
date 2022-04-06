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

package bisq.satoshisquareapp.primary;

import bisq.application.DefaultApplicationService;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.JavaFxApplicationData;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.PrimaryStageController;
import bisq.desktop.primary.onboarding.OnboardingController;
import bisq.satoshisquareapp.primary.main.SatoshiSquareMainController;
import bisq.satoshisquareapp.primary.splash.SatoshiSquareSplashController;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SatoshiSquarePrimaryStageController extends PrimaryStageController {

    public SatoshiSquarePrimaryStageController(DefaultApplicationService applicationService,
                                               JavaFxApplicationData applicationJavaFxApplicationData,
                                               Runnable onStageReadyHandler) {
        super(applicationService, applicationJavaFxApplicationData, onStageReadyHandler);
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case SPLASH -> {
                return Optional.of(new SatoshiSquareSplashController(applicationService));
            }
            case ONBOARDING -> {
                return Optional.of(new OnboardingController(applicationService));
            }
            case MAIN -> {
                return Optional.of(new SatoshiSquareMainController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

  

    @Override
    public void onDomainInitialized() {
        // After the domain is initialized we show the application content
        if (applicationService.getUserProfileService().isDefaultUserProfileMissing()) {
            Navigation.navigateTo(NavigationTarget.ONBOARDING);
        } else {
            Navigation.navigateTo(NavigationTarget.SOCIAL);
        }
    }
}
