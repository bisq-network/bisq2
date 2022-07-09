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

package bisq.desktop.primary.main.content.settings.userProfile.create;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.settings.userProfile.create.step1.GenerateNewProfileStep1Controller;
import bisq.desktop.primary.main.content.settings.userProfile.create.step2.GenerateNewProfileStep2Controller;
import bisq.desktop.primary.overlay.OverlayController;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CreateUserProfileController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final CreateUserProfileModel model;
    @Getter
    private final CreateUserProfileView view;

    public CreateUserProfileController(DefaultApplicationService applicationService) {
        super(NavigationTarget.CREATE_PROFILE);

        this.applicationService = applicationService;
        model = new CreateUserProfileModel();
        view = new CreateUserProfileView(model, this);
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
    public boolean useCaching() {
        return false;
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CREATE_PROFILE_STEP1: {
                return Optional.of(new GenerateNewProfileStep1Controller(applicationService));
            }
            case CREATE_PROFILE_STEP2: {
                return Optional.of(new GenerateNewProfileStep2Controller(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    public void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }
}
