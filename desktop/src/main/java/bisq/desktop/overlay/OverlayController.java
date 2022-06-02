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

package bisq.desktop.overlay;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.newProfilePopup.NewProfilePopupController;
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

    @Getter
    private final OverlayModel model;
    @Getter
    private final OverlayView view;
    private final DefaultApplicationService applicationService;

    public OverlayController(DefaultApplicationService applicationService) {
        super(NavigationTarget.OVERLAY);

        this.applicationService = applicationService;

        model = new OverlayModel(applicationService);
        view = new OverlayView(model, this);
        INSTANCE = this;
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
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case ONBOARDING2 -> {
                return Optional.of(new NewProfilePopupController(applicationService));
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
}
