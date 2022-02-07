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

package bisq.desktop.primary.main.content.social.profile;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.social.components.UserProfileDisplay;
import bisq.desktop.primary.main.content.social.profile.components.CreateUserProfile;
import bisq.desktop.primary.main.content.social.profile.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {
    private final VBox createUserProfile;
    private final BisqButton showCreateUserProfileButton;

    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           UserProfileSelection.View userProfileSelectionView,
                           UserProfileDisplay.View userProfileView,
                           CreateUserProfile.View createUserProfileView) {
        super(new VBox(), model, controller);

        root.setPadding(Layout.PADDING);
        root.setSpacing(40);

        showCreateUserProfileButton = new BisqButton(Res.get("social.createUserProfile.headline"));
        showCreateUserProfileButton.setMinWidth(300);
        createUserProfile = createUserProfileView.getRoot();
        root.getChildren().addAll(userProfileSelectionView.getRoot(),
                userProfileView.getRoot(),
                showCreateUserProfileButton,
                createUserProfile);
    }

    @Override
    public void onViewAttached() {
        showCreateUserProfileButton.setOnAction(e -> controller.showCreateUserProfile());

        showCreateUserProfileButton.visibleProperty().bind(model.createUserProfileVisible.not());
        showCreateUserProfileButton.managedProperty().bind(model.createUserProfileVisible.not());
        createUserProfile.visibleProperty().bind(model.createUserProfileVisible);
        createUserProfile.managedProperty().bind(model.createUserProfileVisible);
    }

    @Override
    protected void onViewDetached() {
        showCreateUserProfileButton.setOnAction(null);
        showCreateUserProfileButton.visibleProperty().unbind();
        showCreateUserProfileButton.managedProperty().unbind();
        createUserProfile.visibleProperty().unbind();
        createUserProfile.managedProperty().unbind();

    }
}
