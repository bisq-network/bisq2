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

import bisq.desktop.common.threading.UIScheduler;
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
    protected final ImageView catHashImageView;
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

        Label headlineLabel = new Label(Res.get("onboarding.createProfile.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.createProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.setMinHeight(40); // does not wrap without that...
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        nickname = new MaterialTextField(Res.get("onboarding.createProfile.nickName"), Res.get("onboarding.createProfile.nickName.prompt"));
        nickname.setMaxWidth(315);

        catHashImageView = new ImageView();
        catHashImageView.setCursor(Cursor.HAND);
        double size = CreateProfileModel.CAT_HASH_IMAGE_SIZE;
        catHashImageView.setFitWidth(size);
        catHashImageView.setFitHeight(catHashImageView.getFitWidth());
        Tooltip.install(catHashImageView, new BisqTooltip(Res.get("onboarding.createProfile.regenerate")));

        double indicatorSize = size / 2;
        powProgressIndicator = new ProgressIndicator();
        powProgressIndicator.setMinSize(indicatorSize, indicatorSize);
        powProgressIndicator.setMaxSize(indicatorSize, indicatorSize);
        powProgressIndicator.setOpacity(0.5);

        StackPane catHashPane = new StackPane();
        catHashPane.setMinSize(size, size);
        catHashPane.setMaxSize(size, size);
        catHashPane.getChildren().addAll(powProgressIndicator, catHashImageView);

        Label titleLabel = new Label(Res.get("onboarding.createProfile.nym").toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-4");

        nym = new Label();
        nym.getStyleClass().add("bisq-text-8");

        VBox nymBox = new VBox(titleLabel, nym);
        nymBox.setAlignment(Pos.CENTER);


        VBox catVBox = new VBox(8, catHashPane, nymBox);
        catVBox.setAlignment(Pos.CENTER);

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

        VBox.setMargin(headlineLabel, new Insets(40, 0, 0, 0));
        root.getChildren().addAll(
                headlineLabel,
                subtitleLabel,
                nickname,
                catVBox,
                buttons
        );
    }

    @Override
    protected void onViewAttached() {
        catHashImageView.imageProperty().bind(model.getCatHashImage());
        catHashImageView.managedProperty().bind(model.getCatHashIconVisible());
        catHashImageView.visibleProperty().bind(model.getCatHashIconVisible());
        powProgressIndicator.managedProperty().bind(model.getCatHashIconVisible().not());
        powProgressIndicator.visibleProperty().bind(model.getCatHashIconVisible().not());
        powProgressIndicator.progressProperty().bind(model.getPowProgress());

        nym.textProperty().bind(model.getNym());
        nym.disableProperty().bind(model.getCatHashIconVisible().not());
        regenerateButton.disableProperty().bind(model.getReGenerateButtonDisabled());
        catHashImageView.mouseTransparentProperty().bind(model.getReGenerateButtonDisabled());
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
        catHashImageView.setOnMouseClicked(e -> controller.onRegenerate());
        createProfileButton.setOnMouseClicked(e -> {
            controller.onCreateUserProfile();
            root.requestFocus();
        });

        UIScheduler.run(nickname::requestFocus).after(700);
    }

    @Override
    protected void onViewDetached() {
        catHashImageView.imageProperty().unbind();
        catHashImageView.managedProperty().unbind();
        catHashImageView.visibleProperty().unbind();
        powProgressIndicator.managedProperty().unbind();
        powProgressIndicator.visibleProperty().unbind();
        powProgressIndicator.progressProperty().unbind();
        feedbackLabel.managedProperty().unbind();
        feedbackLabel.visibleProperty().unbind();

        nym.textProperty().unbind();
        nym.disableProperty().unbind();
        regenerateButton.disableProperty().unbind();
        catHashImageView.mouseTransparentProperty().unbind();
        nickname.mouseTransparentProperty().unbind();

        nickname.textProperty().unbindBidirectional(model.getNickName());

        createProfileButton.disableProperty().unbind();
        createProfileButton.mouseTransparentProperty().unbind();
        createProfileIndicator.managedProperty().unbind();
        createProfileIndicator.visibleProperty().unbind();
        createProfileIndicator.progressProperty().unbind();

        regenerateButton.setOnMouseClicked(null);
        catHashImageView.setOnMouseClicked(null);
        createProfileButton.setOnMouseClicked(null);
    }
}
