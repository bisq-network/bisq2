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

package bisq.desktop.primary.overlay;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.settings.reputation.earnReputation.EarnReputationController;
import bisq.desktop.primary.main.content.settings.userProfile.create.GenerateNewProfileController;
import bisq.desktop.primary.overlay.createOffer.CreateOfferController;
import bisq.desktop.primary.overlay.onboarding.OnboardingController;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Popup for usage for views using the MVC pattern. It is a singleton created by the PrimaryStageController.
 * To add content to the popup we use the Navigation framework. For added views it is transparent if they are used
 * in normal parent views or in a popup.
 */
@Slf4j
public class OverlayController extends NavigationController {
    private static OverlayController INSTANCE;

    public static void hide() {
        INSTANCE.resetSelectedChildTarget();
    }

    public static void setTransitionsType(Transitions.Type transitionsType) {
        INSTANCE.getModel().setTransitionsType(transitionsType);
    }

    @Getter
    private final OverlayModel model;
    @Getter
    private final OverlayView view;
    private final DefaultApplicationService applicationService;

    public OverlayController(DefaultApplicationService applicationService, Region owner) {
        super(NavigationTarget.OVERLAY);

        this.applicationService = applicationService;

        model = new OverlayModel(applicationService);
        view = new OverlayView(model, this, owner);
        INSTANCE = this;
        onActivateInternal();

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                hide();
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    //  Controller implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public void onNavigateToChild(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING -> {
            }
            case CREATE_OFFER -> {
            }
            default -> {
            }
        }
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING -> {
                return Optional.of(new OnboardingController(applicationService));
            }
            case CREATE_OFFER -> {
                return Optional.of(new CreateOfferController(applicationService));
            }
            case CREATE_PROFILE -> {
                return Optional.of(new GenerateNewProfileController(applicationService));
            }
            case EARN_REPUTATION -> {
                return Optional.of(new EarnReputationController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    void onShown() {
    }

    void onHidden() {
        resetSelectedChildTarget();
    }

    public void onQuit() {
        applicationService.shutdown().thenAccept(__ -> Platform.exit());
    }
}
