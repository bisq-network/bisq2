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

package network.misq.desktop.overlay;

import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.common.view.View;

@Slf4j
public class OverlayController {
    private OverlayModel model;

    public OverlayController() {
    }

    public void initialize(Scene parentScene) {
        model = new OverlayModel();
        OverlayView overlayView = new OverlayView(model, this, parentScene);
    }

    public void show(Controller controller) {
        controller.initialize();
        View view = controller.getView();
        model.selectView(view);
    }

    public void onClosed() {
        model.selectView(null);
    }
}
