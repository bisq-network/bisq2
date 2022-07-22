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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class UserProfileView extends View<VBox, UserProfileModel, UserProfileController> {
    private final Button createNewProfileButton;
    private Subscription chatUserDetailsPin;

    public UserProfileView(UserProfileModel model,
                           UserProfileController controller,
                           Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(20);

        Label selectLabel = new Label(Res.get("settings.userProfile.select"));
        selectLabel.getStyleClass().add("bisq-text-3");

        userProfileSelection.setMinHeight(50);
        userProfileSelection.setMaxHeight(userProfileSelection.getMinHeight());

        createNewProfileButton = new Button(Res.get("settings.userProfile.createNewProfile"));
        createNewProfileButton.setDefaultButton(true);
        HBox hBox = new HBox(Spacer.fillHBox(), createNewProfileButton);

        VBox.setMargin(selectLabel, new Insets(20, 0, -20, 0));
        root.getChildren().addAll(selectLabel, userProfileSelection, new Pane(), hBox);
    }

    @Override
    protected void onViewAttached() {
        chatUserDetailsPin = EasyBind.subscribe(model.getUserProfileDisplayPane(), userProfileDisplay -> {
            if (userProfileDisplay != null) {
                VBox.setMargin(userProfileDisplay, new Insets(0, -20, 0, 0));
                root.getChildren().set(2, userProfileDisplay);
            }
        });

        createNewProfileButton.setOnAction(e -> controller.onAddNewChatUser());
    }

    @Override
    protected void onViewDetached() {
        chatUserDetailsPin.unsubscribe();
        createNewProfileButton.setOnAction(null);
    }
}
