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

package bisq.desktop.overlay.onboarding.create_profile;

import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateProfileView extends View<VBox, CreateProfileModel, CreateProfileController> {
    protected final Button regenerateButton;
    protected final Button createProfileButton;
    protected final Label nym;
    protected final ImageView roboIconView;
    protected final ProgressIndicator powProgressIndicator;
    protected final MaterialTextField nickname;
    protected final ProgressIndicator createProfileIndicator;
    private final Label feedbackLabel;

    public CreateProfileView(CreateProfileModel model, CreateProfileController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(25);
        root.setPadding(new Insets(10, 0, 10, 0));
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        Label headLineLabel = new Label(Res.get("onboarding.createProfile.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.createProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.setMinHeight(40); // does not wrap without that...
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        nickname = new MaterialTextField(Res.get("onboarding.createProfile.nickName"), Res.get("onboarding.createProfile.nickName.prompt"));
        nickname.setMaxWidth(315);

        roboIconView = new ImageView();
        roboIconView.setCursor(Cursor.HAND);
        int size = 120;
        roboIconView.setFitWidth(size);
        roboIconView.setFitHeight(size);
        Tooltip.install(roboIconView, new BisqTooltip(Res.get("onboarding.createProfile.regenerate")));

        int indicatorSize = size / 2;
        powProgressIndicator = new ProgressIndicator();
        powProgressIndicator.setMinSize(indicatorSize, indicatorSize);
        powProgressIndicator.setMaxSize(indicatorSize, indicatorSize);
        powProgressIndicator.setOpacity(0.5);

        StackPane roboIconPane = new StackPane();
        roboIconPane.setMinSize(size, size);
        roboIconPane.setMaxSize(size, size);
        roboIconPane.getChildren().addAll(powProgressIndicator, roboIconView);

        Label titleLabel = new Label(Res.get("onboarding.createProfile.nym").toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-4");

        nym = new Label();
        nym.getStyleClass().add("bisq-text-8");

        VBox nymBox = new VBox(titleLabel, nym);
        nymBox.setAlignment(Pos.CENTER);


        VBox roboVBox = new VBox(8, roboIconPane, nymBox);
        roboVBox.setAlignment(Pos.CENTER);

        regenerateButton = new Button(Res.get("onboarding.createProfile.regenerate"));

        createProfileButton = new Button(Res.get("onboarding.createProfile.createProfile"));
        createProfileButton.setGraphicTextGap(8.0);
        createProfileButton.setContentDisplay(ContentDisplay.RIGHT);
        createProfileButton.setDefaultButton(true);

        createProfileIndicator = new ProgressIndicator();
        createProfileIndicator.setProgress(0);
        createProfileIndicator.setMaxWidth(24);
        createProfileIndicator.setMaxHeight(24);
        createProfileIndicator.setManaged(false);
        createProfileIndicator.setVisible(false);

        feedbackLabel = new Label(Res.get("onboarding.createProfile.createProfile.busy"));
        feedbackLabel.setManaged(false);
        feedbackLabel.setVisible(false);
        feedbackLabel.getStyleClass().add("bisq-text-18");

        HBox.setMargin(feedbackLabel, new Insets(0, 0, 0, -10));
        HBox buttons = new HBox(20, regenerateButton, createProfileButton, createProfileIndicator, feedbackLabel);
        buttons.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                nickname,
                roboVBox,
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

        nym.textProperty().bind(model.getNym());
        nym.disableProperty().bind(model.getRoboHashIconVisible().not());
        regenerateButton.disableProperty().bind(model.getReGenerateButtonDisabled());
        roboIconView.mouseTransparentProperty().bind(model.getReGenerateButtonDisabled());
        nickname.mouseTransparentProperty().bind(model.getReGenerateButtonDisabled());

        nickname.textProperty().bindBidirectional(model.getNickName());

        createProfileButton.disableProperty().bind(model.getCreateProfileButtonDisabled());
        createProfileButton.mouseTransparentProperty().bind(model.getCreateProfileButtonDisabled());
        createProfileIndicator.managedProperty().bind(model.getCreateProfileProgress().lessThan(0));
        createProfileIndicator.visibleProperty().bind(model.getCreateProfileProgress().lessThan(0));
        createProfileIndicator.progressProperty().bind(model.getCreateProfileProgress());

        feedbackLabel.managedProperty().bind(model.getCreateProfileProgress().lessThan(0));
        feedbackLabel.visibleProperty().bind(model.getCreateProfileProgress().lessThan(0));

        regenerateButton.setOnMouseClicked(e -> controller.onRegenerate());
        roboIconView.setOnMouseClicked(e -> controller.onRegenerate());
        createProfileButton.setOnMouseClicked(e -> {
            controller.onCreateUserProfile();
            root.requestFocus();
        });

        root.setOnKeyReleased(keyEvent -> KeyHandlerUtil.handleEnterKeyEvent(keyEvent, controller::onCreateUserProfile));

        nickname.requestFocus();
    }

    @Override
    protected void onViewDetached() {
        roboIconView.imageProperty().unbind();
        roboIconView.managedProperty().unbind();
        roboIconView.visibleProperty().unbind();
        powProgressIndicator.managedProperty().unbind();
        powProgressIndicator.visibleProperty().unbind();
        powProgressIndicator.progressProperty().unbind();
        feedbackLabel.managedProperty().unbind();
        feedbackLabel.visibleProperty().unbind();

        nym.textProperty().unbind();
        nym.disableProperty().unbind();
        regenerateButton.disableProperty().unbind();
        roboIconView.mouseTransparentProperty().unbind();
        nickname.mouseTransparentProperty().unbind();

        nickname.textProperty().unbindBidirectional(model.getNickName());

        createProfileButton.disableProperty().unbind();
        createProfileButton.mouseTransparentProperty().unbind();
        createProfileIndicator.managedProperty().unbind();
        createProfileIndicator.visibleProperty().unbind();
        createProfileIndicator.progressProperty().unbind();

        regenerateButton.setOnMouseClicked(null);
        roboIconView.setOnMouseClicked(null);
        createProfileButton.setOnMouseClicked(null);
        root.setOnKeyReleased(null);
    }
}
