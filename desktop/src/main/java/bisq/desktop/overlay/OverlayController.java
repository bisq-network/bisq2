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
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.overlay.window.PopupWindowController;
import javafx.scene.Scene;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OverlayController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final OverlayModel model;
    @Getter
    private final OverlayView view;

    public OverlayController(Scene parentScene, DefaultApplicationService applicationService) {
        super(NavigationTarget.OVERLAY);

        this.applicationService = applicationService;
        model = new OverlayModel();
        view = new OverlayView(model, this, parentScene);
    }

    void onClosed() {
        model.select(NavigationTarget.NONE, null);
    }

    // Not sure if we want to do that here as domains should stay more contained
    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case POPUP_WINDOW -> {
                return Optional.of(new PopupWindowController(applicationService));
                //return Optional.of(new OverlayController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
