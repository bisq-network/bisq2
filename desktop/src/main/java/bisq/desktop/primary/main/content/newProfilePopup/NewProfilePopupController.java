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
import bisq.desktop.overlay.BasicOverlay;
import bisq.desktop.primary.main.content.newProfilePopup.createOffer.CreateOfferController;
import bisq.desktop.primary.main.content.newProfilePopup.initNymProfile.InitNymProfileController;
import bisq.desktop.primary.main.content.newProfilePopup.selectUserType.SelectUserTypeController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;

@Slf4j
public class NewProfilePopupController implements Controller {
    private final NewProfilePopupModel model;
    @Getter
    private final NewProfilePopupView view;
    private final BasicOverlay popup;

    List<Controller> stepsControllers;
    Subscription stepSubscription;

    public NewProfilePopupController(BasicOverlay popup,DefaultApplicationService applicationService) {
        model = new NewProfilePopupModel();
        this.popup = popup;
        view = new NewProfilePopupView(model, this, popup);

        stepsControllers = List.of(
                new InitNymProfileController(applicationService, this::onSubViewNavigationChange),
                new SelectUserTypeController(applicationService, this::onSubViewNavigationChange),
                new CreateOfferController(applicationService, this::onSubViewNavigationChange)
        );
    }

    public void show() {
        popup.show();
        view.addContent();
        view.onViewAttached();
        onActivate();
    }
    @Override
    public void onActivate() {
        model.currentStepProperty().set(0);
        stepSubscription = EasyBind.subscribe(model.currentStepProperty(), step -> view.setupSelectedStep());
    }

    @Override
    public void onDeactivate() {
        stepSubscription.unsubscribe();
    }

    protected void addContent() {
        view.addContent();
    }

    public void skipStep() {
        if (model.isLastStep()) {
            popup.hide();
        } else {
            model.increaseStep();
        }
    }

    private void onSubViewNavigationChange(int direction) {
        if (direction == 1) {
            model.increaseStep();
        } else if (direction == -1) {
            model.decreaseStep();
        }
    }
}
