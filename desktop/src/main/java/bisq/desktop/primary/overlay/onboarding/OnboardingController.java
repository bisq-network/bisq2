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

package bisq.desktop.primary.overlay.onboarding;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.create_profile.CreateProfileController;
import bisq.desktop.primary.overlay.onboarding.password.OnboardingPasswordController;
import bisq.desktop.primary.overlay.onboarding.welcome.WelcomeController;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OnboardingController extends NavigationController {
    private final ServiceProvider serviceProvider;
    @Getter
    private final OnboardingModel model;
    @Getter
    private final OnboardingView view;

    public OnboardingController(ServiceProvider serviceProvider) {
        super(NavigationTarget.ONBOARDING);

        this.serviceProvider = serviceProvider;
        model = new OnboardingModel();
        view = new OnboardingView(model, this);
    }

    @Override
    public void onActivate() {
        OverlayController.setTransitionsType(Transitions.Type.LIGHT);
    }

    @Override
    public void onDeactivate() {
        OverlayController.setTransitionsType(Transitions.DEFAULT_TYPE);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING_WELCOME: {
                return Optional.of(new WelcomeController(serviceProvider));
            }
            case ONBOARDING_GENERATE_NYM: {
                return Optional.of(new CreateProfileController(serviceProvider));
            }
            case ONBOARDING_PASSWORD: {
                return Optional.of(new OnboardingPasswordController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    public void onQuit() {
        serviceProvider.getShotDownHandler().shutdown().thenAccept(result -> Platform.exit());
    }
}
