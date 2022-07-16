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

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.primary.overlay.OverlayModel;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateUserProfileView extends NavigationView<VBox, CreateUserProfileModel, CreateUserProfileController> {
    public CreateUserProfileView(CreateUserProfileModel model, CreateUserProfileController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        model.getView().addListener((observable, oldValue, newValue) -> {
            Region childRoot = newValue.getRoot();
            childRoot.setPrefHeight(root.getHeight());
            root.getChildren().add(childRoot);
            if (oldValue != null) {
                Transitions.transitLeftOut(childRoot, oldValue.getRoot());
            } else {
                Transitions.fadeIn(childRoot);
            }
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
