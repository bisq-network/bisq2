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
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {

    private final Pane editUserProfilePane;
    private final Button addNewUserButton;
    private Subscription chatUserDetailsPin;

    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           UserProfileSelection userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.setPadding(new Insets(20));

        Label selectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        selectLabel.getStyleClass().add("bisq-text-4");
        VBox selectionVBox = new VBox(0, selectLabel, userProfileSelection.getRoot());

        editUserProfilePane = new Pane();

        addNewUserButton = new Button(Res.get("settings.userProfile.addNewUser"));
        addNewUserButton.setDefaultButton(true);
        
        root.getChildren().addAll(selectionVBox, editUserProfilePane, addNewUserButton);
    }

    @Override
    protected void onViewAttached() {
        chatUserDetailsPin = EasyBind.subscribe(model.getEditUserProfile(), editUserProfile -> {
            editUserProfilePane.getChildren().setAll(editUserProfile.getRoot());
        });

        addNewUserButton.setOnAction(e -> controller.onAddNewChatUser());
    }

    @Override
    protected void onViewDetached() {
        chatUserDetailsPin.unsubscribe();
        addNewUserButton.setOnAction(null);
    }
}
