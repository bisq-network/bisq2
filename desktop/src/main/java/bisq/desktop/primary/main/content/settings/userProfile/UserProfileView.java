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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.desktop.common.view.View;
import javafx.scene.control.Button;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {
    private final Button showCreateUserProfileButton;
    private final Pane channelAdmin;
    private final Pane createUserProfile;

    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           Pane userProfileSelection,
                           Pane userProfile,
                           Pane channelAdmin,
                           Pane createUserProfile) {
        super(new VBox(), model, controller);
        this.channelAdmin = channelAdmin;
        this.createUserProfile = createUserProfile;

        root.setPadding(Layout.PADDING);
        root.setSpacing(40);

        showCreateUserProfileButton = new Button(Res.get("social.createUserProfile.headline"));
        showCreateUserProfileButton.setMinWidth(300);
        userProfileSelection.setMinWidth(600);
        root.getChildren().addAll(userProfileSelection,
                userProfile,
                showCreateUserProfileButton,
                channelAdmin,
                createUserProfile);
    }

    @Override
    protected void onViewAttached() {
        showCreateUserProfileButton.setOnAction(e -> controller.showCreateUserProfile());

        showCreateUserProfileButton.visibleProperty().bind(model.createUserProfileVisible.not());
        showCreateUserProfileButton.managedProperty().bind(model.createUserProfileVisible.not());
        channelAdmin.visibleProperty().bind(model.channelAdminVisible);
        channelAdmin.managedProperty().bind(model.channelAdminVisible);
        createUserProfile.visibleProperty().bind(model.createUserProfileVisible);
        createUserProfile.managedProperty().bind(model.createUserProfileVisible);
    }

    @Override
    protected void onViewDetached() {
        showCreateUserProfileButton.setOnAction(null);
        showCreateUserProfileButton.visibleProperty().unbind();
        showCreateUserProfileButton.managedProperty().unbind();
        channelAdmin.visibleProperty().unbind();
        channelAdmin.managedProperty().unbind();
        createUserProfile.visibleProperty().unbind();
        createUserProfile.managedProperty().unbind();
    }
}
