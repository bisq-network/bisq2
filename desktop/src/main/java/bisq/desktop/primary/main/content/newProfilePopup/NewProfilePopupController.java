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

package bisq.desktop.primary.main.content.newProfilePopup;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.newProfilePopup.createOffer.CreateOfferControllerOld;
import bisq.desktop.primary.overlay.onboarding.profile.CreateProfileController;
import bisq.desktop.primary.main.content.newProfilePopup.selectUserType.SelectUserTypeController;
import bisq.desktop.primary.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NewProfilePopupController implements Controller {
    private final DefaultApplicationService applicationService;
    private final NewProfilePopupModel model;
    @Getter
    private final NewProfilePopupView view;
    private Subscription stepSubscription;

    public NewProfilePopupController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new NewProfilePopupModel();
        view = new NewProfilePopupView(model, this);
    }

    @Override
    public void onActivate() {
        model.currentStepProperty().set(0);
        stepSubscription = EasyBind.subscribe(model.currentStepProperty(),
                stepAsNumber -> {
                    int step = (int) stepAsNumber;
                    if (step == 0) {
                        CreateProfileController controller = new CreateProfileController(applicationService);
                        model.setView(controller.getView());
                    } else if (step == 1) {
                        SelectUserTypeController controller = new SelectUserTypeController(applicationService, this::navigateSubView);
                        model.setView(controller.getView());
                    } else if (step == 2) {
                        CreateOfferControllerOld controller = new CreateOfferControllerOld(applicationService, this::navigateSubView);
                        model.setView(controller.getView());
                    }
                });


    }

    @Override
    public void onDeactivate() {
        stepSubscription.unsubscribe();
    }

    public void onSkip() {
        OverlayController.hide();
    }

    private void navigateSubView(boolean isNext) {
        if (isNext) {
            model.increaseStep();
        } else {
            model.decreaseStep();
        }
    }
}
