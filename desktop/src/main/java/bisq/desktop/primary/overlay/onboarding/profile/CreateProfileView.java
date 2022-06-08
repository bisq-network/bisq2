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
import bisq.desktop.components.controls.TextInputBox;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateProfileView extends View<VBox, CreateProfileModel, CreateProfileController> {
    private final Button regenerateButton;
    private final Button nextButton;
    private final TextInputBox nicknameTextInputBox;
    private final Label nymId;
    private final ImageView roboIconView;
    private final ProgressIndicator powProgressIndicator;

    public CreateProfileView(CreateProfileModel model, CreateProfileController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.getStyleClass().add("bisq-content-bg");
        root.setPadding(new Insets(10, 0, 30, 0));

        Label headLineLabel = new Label(Res.get("createProfile.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("createProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(300);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 8, 0));

        roboIconView = new ImageView();
        roboIconView.setCursor(Cursor.HAND);
        int size = 128;
        roboIconView.setFitWidth(size);
        roboIconView.setFitHeight(size);

        powProgressIndicator = new ProgressIndicator();
        powProgressIndicator.setMinSize(size, size);
        powProgressIndicator.setOpacity(0.5);

        VBox profileIdBox = getValueBox(Res.get("createProfile.nymId"));
        VBox.setMargin(profileIdBox, new Insets(0, 0, 16, 0));
        nymId = (Label) profileIdBox.getChildren().get(1);

        regenerateButton = new Button(Res.get("createProfile.regenerate"));
        regenerateButton.getStyleClass().setAll("bisq-transparent-button", "bisq-text-3", "text-underline");
        VBox.setMargin(regenerateButton, new Insets(0, 0, 16, 0));

        nicknameTextInputBox = new TextInputBox(Res.get("createProfile.nickName"),
                Res.get("createProfile.nickName.prompt"));
        nicknameTextInputBox.setPrefWidth(300);
        VBox.setMargin(nicknameTextInputBox, new Insets(0, 0, 16, 0));


        nextButton = new Button(Res.get("createProfile.createProfile"));
        nextButton.setGraphicTextGap(8.0);
        nextButton.setContentDisplay(ContentDisplay.RIGHT);
        nextButton.setDefaultButton(true);

        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                powProgressIndicator,
                roboIconView,
                profileIdBox,
                regenerateButton,
                nicknameTextInputBox,
                nextButton
        );
    }

    @Override
    protected void onViewAttached() {
        nicknameTextInputBox.textProperty().bindBidirectional(model.nickName);
        nicknameTextInputBox.requestFocus();

        nymId.textProperty().bind(model.nymId);
        nextButton.graphicProperty().bind(Bindings.createObjectBinding(() -> {
            if (!model.isBusy.get()) {
                return null;
            }
            ProgressIndicator indicator = new ProgressIndicator();
            indicator.setProgress(-1f);
            indicator.setMaxWidth(24.0);
            indicator.setMaxHeight(24.0);
            return indicator;
        }, model.isBusy));
        nextButton.disableProperty().bind(model.createProfileButtonDisable);


        roboIconView.imageProperty().bind(model.roboHashImage);
        roboIconView.managedProperty().bind(model.roboHashIconVisible);
        roboIconView.visibleProperty().bind(model.roboHashIconVisible);
        powProgressIndicator.managedProperty().bind(model.roboHashIconVisible.not());
        powProgressIndicator.visibleProperty().bind(model.roboHashIconVisible.not());
        powProgressIndicator.progressProperty().bind(model.powProgress);
        regenerateButton.disableProperty().bind(model.roboHashIconVisible.not());
        nymId.disableProperty().bind(model.roboHashIconVisible.not());

        regenerateButton.setOnAction(e -> controller.onCreateTempIdentity());
        nextButton.setOnAction(e -> controller.onCreateNymProfile());
        
        // We deactivate the close popup by escape
        root.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                e.consume();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        nicknameTextInputBox.textProperty().unbindBidirectional(model.nickName);
        nextButton.graphicProperty().unbind();
        nextButton.disableProperty().unbind();
        roboIconView.imageProperty().unbind();
        roboIconView.managedProperty().unbind();
        roboIconView.visibleProperty().unbind();
        powProgressIndicator.managedProperty().unbind();
        powProgressIndicator.visibleProperty().unbind();
        powProgressIndicator.progressProperty().unbind();
        regenerateButton.disableProperty().unbind();
        nymId.disableProperty().unbind();

        regenerateButton.setOnAction(null);
        nextButton.setOnAction(null);
    }

    private VBox getValueBox(String title) {
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-4");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("bisq-text-1");

        VBox box = new VBox(titleLabel, valueLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
