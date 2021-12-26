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

package network.misq.desktop.main.left;

import lombok.Getter;
import network.misq.api.DefaultApi;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentViewController;
import network.misq.desktop.main.content.createoffer.CreateOfferController;
import network.misq.desktop.overlay.OverlayController;

public class NavigationViewController implements Controller {
    private NavigationViewModel model;
    @Getter
    private NavigationView view;
    private final DefaultApi api;
    private final ContentViewController contentViewController;
    private final OverlayController overlayController;

    public NavigationViewController(DefaultApi api,
                                    ContentViewController contentViewController,
                                    OverlayController overlayController) {
        this.api = api;
        this.contentViewController = contentViewController;
        this.overlayController = overlayController;
    }

    @Override
    public void initialize() {
        this.model = new NavigationViewModel();
        this.view = new NavigationView(model, this);
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }

    public void onShowView(Class<? extends Controller> controllerClass) {
        if (controllerClass == CreateOfferController.class) {
            overlayController.show(new CreateOfferController(api));
        } else {
            contentViewController.onNavigationRequest(controllerClass);
        }
    }

}
