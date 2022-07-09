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
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {
    private final Button createNewProfileButton, deleteProfileButton;
    private Subscription chatUserDetailsPin;

    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.setPadding(new Insets(20));

        Label selectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        selectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelection.setMinHeight(50);
        VBox.setVgrow(selectLabel, Priority.ALWAYS);
        VBox selectionVBox = new VBox(0, selectLabel, userProfileSelection);

        createNewProfileButton = new Button(Res.get("settings.userProfile.createNewProfile"));
        createNewProfileButton.setDefaultButton(true);
        createNewProfileButton.setMinWidth(300);

        deleteProfileButton = new Button(Res.get("settings.userProfile.deleteProfile"));    //todo
        deleteProfileButton.setDefaultButton(false);
        deleteProfileButton.setMinWidth(300);

        VBox.setMargin(createNewProfileButton, new Insets(-15,0,0,0));
        root.getChildren().addAll(selectionVBox, new Pane(), createNewProfileButton, deleteProfileButton, Spacer.fillVBox());
    }

    @Override
    protected void onViewAttached() {
        chatUserDetailsPin = EasyBind.subscribe(model.getUserProfileDisplayPane(), editUserProfilePane -> {
            editUserProfilePane.setMaxWidth(300);
            VBox.setMargin(editUserProfilePane, new Insets(-40, 0, 0, 0));
            VBox.setVgrow(editUserProfilePane, Priority.ALWAYS);
            root.getChildren().set(1, editUserProfilePane);
        });

        createNewProfileButton.setOnAction(e -> controller.onAddNewChatUser());
        deleteProfileButton.setOnAction(e -> controller.onDeleteChatUser());
    }

    @Override
    protected void onViewDetached() {
        chatUserDetailsPin.unsubscribe();
        createNewProfileButton.setOnAction(null);
        deleteProfileButton.setOnAction(null);
    }
}
