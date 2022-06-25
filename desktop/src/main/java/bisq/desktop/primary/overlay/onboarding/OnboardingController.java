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

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.bisq2.Bisq2IntroController;
import bisq.desktop.primary.overlay.onboarding.profile.nym.GenerateNymController;
import javafx.application.Platform;
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
        model = new OnboardingModel();
        view = new OnboardingView(model, this);
    }

    @Override
    public void onNavigateToChild(NavigationTarget navigationTarget) {
    }

    @Override
    public void onActivate() {
        OverlayController.setTransitionsType(Transitions.Type.LIGHT);
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING_BISQ_2_INTRO -> {
                return Optional.of(new Bisq2IntroController(applicationService));
            }
            case ONBOARDING_GENERATE_NYM -> {
                return Optional.of(new GenerateNymController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    public void onQuit() {
        applicationService.shutdown()
                .thenAccept(__ -> Platform.exit());
    }
}
