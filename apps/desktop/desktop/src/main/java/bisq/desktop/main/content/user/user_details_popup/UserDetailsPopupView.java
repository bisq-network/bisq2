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

package bisq.desktop.main.content.user.user_details_popup;

import bisq.desktop.common.view.TabView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.overlay.OverlayModel;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class UserDetailsPopupView extends TabView<UserDetailsPopupModel, UserDetailsPopupController> {
    private Button closeButton;
    private UserProfileDisplay userProfileDisplay;
    private Subscription reputationScorePin;

    public UserDetailsPopupView(UserDetailsPopupModel model, UserDetailsPopupController controller) {
        super(model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.getStyleClass().add("user-details-popup");
    }

    @Override
    protected void onViewAttached() {
        userProfileDisplay.setUserProfile(model.getUserProfile());
        userProfileDisplay.setReputationScoreDisplayScale(1.5);

        reputationScorePin = EasyBind.subscribe(model.getReputationScore(), reputationScore -> {
            if (reputationScore != null) {
                userProfileDisplay.setReputationScore(reputationScore);
            }
        });

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        reputationScorePin.unsubscribe();
        closeButton.setOnAction(null);
    }

    @Override
    protected void setupTopBox() {
        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        userProfileDisplay = new UserProfileDisplay(100);
        userProfileDisplay.setPadding(new Insets(0, 0, 40, 80));

        topBox = new VBox();
        topBox.getChildren().addAll(closeButtonRow, userProfileDisplay, tabs);
    }
}
