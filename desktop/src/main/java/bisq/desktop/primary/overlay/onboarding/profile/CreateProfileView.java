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

package bisq.desktop.primary.overlay.onboarding.profile;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.TextAreaBox;
import bisq.desktop.components.controls.TextInputBox;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateProfileView extends View<VBox, CreateProfileModel, CreateProfileController> {
    private final Button createProfileButton;
    private final TextInputBox nicknameTextInputBox;
    private final TextAreaBox bioTextAreaBox, termsTextAreaBox;

    public CreateProfileView(CreateProfileModel model, CreateProfileController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 30, 0));

        Label headLineLabel = new Label(Res.get("createProfile.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("createProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");


        nicknameTextInputBox = new TextInputBox(Res.get("createProfile.nickName"),
                Res.get("createProfile.nickName.prompt"));
        nicknameTextInputBox.setPrefWidth(400);

        bioTextAreaBox = new TextAreaBox(Res.get("createProfile.bio").toUpperCase(),
                Res.get("createProfile.bio.prompt"));
        bioTextAreaBox.setPrefWidth(400);
        bioTextAreaBox.setBoxHeight(70);

        termsTextAreaBox = new TextAreaBox(Res.get("createProfile.terms").toUpperCase(),
                Res.get("createProfile.terms.prompt"));
        termsTextAreaBox.setPrefWidth(400);
        termsTextAreaBox.setBoxHeight(100);

        createProfileButton = new Button(Res.get("createProfile.createProfile"));
        createProfileButton.setGraphicTextGap(8.0);
        createProfileButton.setContentDisplay(ContentDisplay.RIGHT);
        createProfileButton.setDefaultButton(true);

        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 3, 0));
        VBox.setMargin(nicknameTextInputBox, new Insets(0, 0, 15, 0));
        VBox.setMargin(bioTextAreaBox, new Insets(0, 0, 15, 0));
        VBox.setMargin(termsTextAreaBox, new Insets(0, 0, 15, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                nicknameTextInputBox,
                bioTextAreaBox,
                termsTextAreaBox,
                createProfileButton
        );
    }

    @Override
    protected void onViewAttached() {
        nicknameTextInputBox.textProperty().bindBidirectional(model.nickName);
        bioTextAreaBox.textProperty().bindBidirectional(model.bio);
        termsTextAreaBox.textProperty().bindBidirectional(model.terms);
        createProfileButton.disableProperty().bind(model.createProfileButtonDisabled);
        createProfileButton.mouseTransparentProperty().bind(model.createProfileButtonDisabled);
        createProfileButton.setOnAction(e -> controller.onCreateUserProfile());

        nicknameTextInputBox.requestFocus();
    }

    @Override
    protected void onViewDetached() {
        nicknameTextInputBox.textProperty().unbindBidirectional(model.nickName);
        createProfileButton.disableProperty().unbind();
        createProfileButton.mouseTransparentProperty().unbind();
        createProfileButton.setOnAction(null);
    }
}
