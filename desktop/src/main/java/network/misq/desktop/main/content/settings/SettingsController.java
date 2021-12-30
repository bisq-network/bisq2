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
import network.misq.api.DefaultApi;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentViewController;
import network.misq.desktop.overlay.OverlayController;

public class SettingsController implements Controller {
    private SettingsModel model;
    @Getter
    private SettingsView view;
    @Getter
    private final DefaultApi api;
    private final ContentViewController contentViewController;
    private final OverlayController overlayController;

    public SettingsController(DefaultApi api, ContentViewController contentViewController, OverlayController overlayController) {
        this.api = api;
        this.contentViewController = contentViewController;
        this.overlayController = overlayController;
    }

    @Override
    public void initialize() {
        model = new SettingsModel(api);
        model.initialize();
        view = new SettingsView(model, this);
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
