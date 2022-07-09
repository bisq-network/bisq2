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
import bisq.desktop.primary.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenerateProfileView extends View<VBox, GenerateProfileModel, GenerateProfileController> {
    protected final Button regenerateButton;
    protected final Button createProfileButton;
    protected final Label nymId;
    protected final ImageView roboIconView;
    protected final ProgressIndicator powProgressIndicator;
    protected final TextInputBox nicknameTextInputBox;
    protected final ProgressIndicator createProfileIndicator;

    public GenerateProfileView(GenerateProfileModel model, GenerateProfileController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 10, 0));
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        Label headLineLabel = new Label(Res.get("generateNym.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("generateNym.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.setMinHeight(40); // does not wrap without that...
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        roboIconView = new ImageView();
        roboIconView.setCursor(Cursor.HAND);
        int size = 128;
        roboIconView.setFitWidth(size);
        roboIconView.setFitHeight(size);

        int indicatorSize = size / 2;
        powProgressIndicator = new ProgressIndicator();
        powProgressIndicator.setMinSize(indicatorSize, indicatorSize);
        powProgressIndicator.setMaxSize(indicatorSize, indicatorSize);
        powProgressIndicator.setOpacity(0.5);

        int width = 250;
        StackPane roboIconPane = new StackPane();
        roboIconPane.setMinSize(width, size);
        roboIconPane.setMaxSize(width, size);
        roboIconPane.getChildren().addAll(powProgressIndicator, roboIconView);

        Label titleLabel = new Label(Res.get("generateNym.nymId").toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-4");

        nymId = new Label();
        nymId.getStyleClass().add("bisq-text-8");

        VBox nymIdBox = new VBox(titleLabel, nymId);
        nymIdBox.setAlignment(Pos.CENTER);

        nicknameTextInputBox = new TextInputBox(Res.get("addNickName.nickName"), Res.get("addNickName.nickName.prompt"));
        nicknameTextInputBox.setMinWidth(width);
        nicknameTextInputBox.setMaxWidth(width);

        VBox roboVBox = new VBox(8, roboIconPane, nymIdBox);
        roboVBox.setAlignment(Pos.CENTER);
        roboVBox.setPrefWidth(width);
        roboVBox.setPrefHeight(200);

        HBox.setMargin(nicknameTextInputBox, new Insets(-20, 0, 0, 0));
        HBox centerHBox = new HBox(10, roboVBox, nicknameTextInputBox);
        centerHBox.setAlignment(Pos.CENTER);

        regenerateButton = new Button(Res.get("generateNym.regenerate"));

        createProfileButton = new Button(Res.get("generateNym.createProfile"));
        createProfileButton.setGraphicTextGap(8.0);
        createProfileButton.setContentDisplay(ContentDisplay.RIGHT);
        createProfileButton.setDefaultButton(true);

        createProfileIndicator = new ProgressIndicator();
        createProfileIndicator.setProgress(0);
        createProfileIndicator.setMaxWidth(24);
        createProfileIndicator.setMaxHeight(24);
        createProfileIndicator.setManaged(false);
        createProfileIndicator.setVisible(false);

        HBox buttons = new HBox(20, regenerateButton, createProfileButton, createProfileIndicator);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 40, 0));
        VBox.setMargin(buttons, new Insets(60, 0, 0, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                centerHBox,
                buttons
        );
    }

    @Override
    protected void onViewAttached() {
        roboIconView.imageProperty().bind(model.getRoboHashImage());
        roboIconView.managedProperty().bind(model.getRoboHashIconVisible());
        roboIconView.visibleProperty().bind(model.getRoboHashIconVisible());
        powProgressIndicator.managedProperty().bind(model.getRoboHashIconVisible().not());
        powProgressIndicator.visibleProperty().bind(model.getRoboHashIconVisible().not());
        powProgressIndicator.progressProperty().bind(model.getPowProgress());

        nymId.textProperty().bind(model.getProfileId());
        nymId.disableProperty().bind(model.getRoboHashIconVisible().not());
        regenerateButton.disableProperty().bind(model.getReGenerateButtonDisabled());
        roboIconView.mouseTransparentProperty().bind(model.getReGenerateButtonDisabled());
        nicknameTextInputBox.mouseTransparentProperty().bind(model.getReGenerateButtonDisabled());

        nicknameTextInputBox.textProperty().bindBidirectional(model.getNickName());

        createProfileButton.disableProperty().bind(model.getCreateProfileButtonDisabled());
        createProfileButton.mouseTransparentProperty().bind(model.getCreateProfileButtonDisabled());
        createProfileIndicator.managedProperty().bind(model.getCreateProfileProgress().lessThan(0));
        createProfileIndicator.visibleProperty().bind(model.getCreateProfileProgress().lessThan(0));
        createProfileIndicator.progressProperty().bind(model.getCreateProfileProgress());

        regenerateButton.setOnAction(e -> controller.onRegenerate());
        roboIconView.setOnMouseClicked(e -> controller.onRegenerate());
        createProfileButton.setOnAction(e -> {
            controller.onCreateUserProfile();
            root.requestFocus();
        });

        nicknameTextInputBox.requestFocus();
    }

    @Override
    protected void onViewDetached() {
        roboIconView.imageProperty().unbind();
        roboIconView.managedProperty().unbind();
        roboIconView.visibleProperty().unbind();
        powProgressIndicator.managedProperty().unbind();
        powProgressIndicator.visibleProperty().unbind();
        powProgressIndicator.progressProperty().unbind();

        nymId.textProperty().unbind();
        nymId.disableProperty().unbind();
        regenerateButton.disableProperty().unbind();
        roboIconView.mouseTransparentProperty().unbind();
        nicknameTextInputBox.mouseTransparentProperty().unbind();

        nicknameTextInputBox.textProperty().unbindBidirectional(model.getNickName());

        createProfileButton.disableProperty().unbind();
        createProfileButton.mouseTransparentProperty().unbind();
        createProfileIndicator.managedProperty().unbind();
        createProfileIndicator.visibleProperty().unbind();
        createProfileIndicator.progressProperty().unbind();

        regenerateButton.setOnAction(null);
        roboIconView.setOnMouseClicked(null);
        createProfileButton.setOnAction(null);
    }
}
