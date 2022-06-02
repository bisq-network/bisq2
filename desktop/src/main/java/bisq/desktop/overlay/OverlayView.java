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

import bisq.desktop.common.view.NavigationView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OverlayView extends NavigationView<Pane, OverlayModel, OverlayController> {
    public OverlayView(OverlayModel model, OverlayController controller) {
        super(new Pane(), model, controller);

        model.getView().addListener((observable, oldValue, newValue) -> {
            log.error("Add view {}", newValue);
            controller.root = (Pane) newValue.getRoot();
            controller.base.getChildren().add(newValue.getRoot());
            controller.show();
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}