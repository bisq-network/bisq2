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
import bisq.desktop.primary.overlay.onboarding.bisqeasy.BisqEasyIntroController;
import bisq.desktop.primary.overlay.onboarding.nym.GenerateNymController;
import bisq.desktop.primary.overlay.onboarding.offer.CreateOfferController;
import bisq.desktop.primary.overlay.onboarding.profile.CreateProfileController;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class OnboardingController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final OnboardingModel model;
    @Getter
    private final OnboardingView view;
    private final CreateProfileController createProfileController;
    private final GenerateNymController generateNymController;
    private Subscription nickNamePin, bioPin, termsPin;

    public OnboardingController(DefaultApplicationService applicationService) {
        super(NavigationTarget.ONBOARDING);

        this.applicationService = applicationService;
        model = new OnboardingModel();
        view = new OnboardingView(model, this);

        createProfileController = new CreateProfileController(applicationService);
        generateNymController = new GenerateNymController(applicationService);
    }

    @Override
    public void onNavigateToChild(NavigationTarget navigationTarget) {
    }

    @Override
    public void onActivate() {
        OverlayController.setTransitionsType(Transitions.Type.LIGHT);

        nickNamePin = EasyBind.subscribe(createProfileController.getNickName(), generateNymController::setNickName);
        bioPin = EasyBind.subscribe(createProfileController.getBio(), generateNymController::setBio);
        termsPin = EasyBind.subscribe(createProfileController.getTerms(), generateNymController::setTerms);
    }

    @Override
    public void onDeactivate() {
        nickNamePin.unsubscribe();
        bioPin.unsubscribe();
        termsPin.unsubscribe();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING_BISQ_2_INTRO -> {
                return Optional.of(new Bisq2IntroController(applicationService));
            }
            case ONBOARDING_CREATE_PROFILE -> {
                return Optional.of(createProfileController);
            }
            case ONBOARDING_GENERATE_NYM -> {
                return Optional.of(generateNymController);
            }
            case ONBOARDING_BISQ_EASY -> {
                return Optional.of(new BisqEasyIntroController(applicationService));
            }
            case CREATE_OFFER -> {
                return Optional.of(new CreateOfferController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    public void onSkip() {
        OverlayController.hide();
    }

    public void onQuit() {
        applicationService.shutdown()
                .thenAccept(__ -> Platform.exit());
    }
}
