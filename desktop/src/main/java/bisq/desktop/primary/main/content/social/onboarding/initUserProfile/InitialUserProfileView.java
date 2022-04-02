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

package bisq.desktop.primary.main.content.social.onboarding.initUserProfile;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class InitialUserProfileView extends View<VBox, InitialUserProfileModel, InitUserProfileController> {
    public static Pos alignment = Pos.TOP_LEFT;
    public static TextAlignment textAlignment = TextAlignment.LEFT;

    private final ImageView roboIconImageView;
    private final BisqButton createUserButton;
    private final BisqLabel feedbackLabel;
    private final Button tryOtherButton;
    private final BisqTextField userNameInputField;
    private Subscription roboHashNodeSubscription;

    public InitialUserProfileView(InitialUserProfileModel model, InitUserProfileController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(alignment);
        root.getStyleClass().add("content-pane");

        BisqLabel headline = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.headline"));
        headline.getStyleClass().add("headline-label");
        VBox.setMargin(headline, new Insets(20, 0, -20, 0));

        roboIconImageView = new ImageView();

        userNameInputField = new BisqTextField();
        userNameInputField.setMaxWidth(300);
        userNameInputField.setEditable(false);
        userNameInputField.setFocusTraversable(false);
        userNameInputField.setMouseTransparent(false);
        userNameInputField.setPromptText(Res.get("social.createUserProfile.userName.prompt"));
        VBox.setMargin(userNameInputField, new Insets(10, 0, 10, 0));

        BisqLabel infoLabel = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.info"));
        infoLabel.setTextAlignment(textAlignment);
        VBox.setMargin(infoLabel, new Insets(0, 0, 20, 0));

        BisqLabel tryOtherInfoLabel = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.info"));
        tryOtherInfoLabel.setTextAlignment(textAlignment);
        tryOtherButton = new BisqButton(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.button"));

        createUserButton = new BisqButton(Res.get("satoshisquareapp.setDefaultUserProfile.done"));
        createUserButton.disableProperty().bind(userNameInputField.textProperty().isEmpty());
        createUserButton.getStyleClass().add("action-button");

        feedbackLabel = new BisqLabel("sfdasfa");
        feedbackLabel.setWrapText(true);

        root.getChildren().addAll(
                headline,
                roboIconImageView,
                userNameInputField,
                tryOtherInfoLabel,
                tryOtherButton,
                createUserButton,
                feedbackLabel,
                infoLabel
        );
    }

    @Override
    protected void onViewAttached() {
        tryOtherButton.disableProperty().bind(model.tryOtherButtonDisable);
        createUserButton.disableProperty().bind(model.createProfileButtonDisable);
        userNameInputField.textProperty().bindBidirectional(model.userName);
        feedbackLabel.textProperty().bind(model.feedback);

        tryOtherButton.setOnAction(e -> controller.onCreateTempIdentity());
        createUserButton.setOnAction(e -> controller.onCreateUserProfile());

        roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
            if (roboIcon != null) {
                roboIconImageView.setImage(roboIcon);
            }
            roboIconImageView.setVisible(roboIcon != null);
        });
    }

    @Override
    protected void onViewDetached() {
        tryOtherButton.disableProperty().unbind();
        createUserButton.disableProperty().unbind();
        userNameInputField.textProperty().unbindBidirectional(model.userName);
        feedbackLabel.textProperty().unbind();

        tryOtherButton.setOnAction(null);
        createUserButton.setOnAction(null);

        roboHashNodeSubscription.unsubscribe();
    }
}
