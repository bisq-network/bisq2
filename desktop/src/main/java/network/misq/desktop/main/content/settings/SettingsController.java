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

package network.misq.desktop.main.content.settings;

import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentViewController;
import network.misq.desktop.overlay.OverlayController;

public class SettingsController implements Controller {
    private final SettingsModel model;
    @Getter
    private final SettingsView view;
    private final DefaultServiceProvider serviceProvider;
    private final ContentViewController contentViewController;
    private final OverlayController overlayController;

    public SettingsController(DefaultServiceProvider serviceProvider, ContentViewController contentViewController, OverlayController overlayController) {
         this.serviceProvider = serviceProvider;
        model = new SettingsModel(serviceProvider);
        view = new SettingsView(model, this);
        
        this.contentViewController = contentViewController;
        this.overlayController = overlayController;
    }

    @Override
    public void initialize() {
        model.initialize();
    }

    @Override
    public void onViewAdded() {
        model.activate();

        // Platform.runLater(() -> onCreateOffer());
    }

    @Override
    public void onViewRemoved() {
        model.deactivate();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

}
