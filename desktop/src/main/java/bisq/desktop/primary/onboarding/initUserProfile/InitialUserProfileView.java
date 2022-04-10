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

package bisq.desktop.primary.onboarding.initUserProfile;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class InitialUserProfileView extends View<ScrollPane, InitialUserProfileModel, InitUserProfileController> {
    private final ImageView roboIconImageView;
    private final BisqButton createUserButton;
    private final BisqLabel processingLabel;
    private final Label generatedUserName;
    private final VBox vBox;
    private final TextField nickNameTextField;
    private Subscription roboHashNodeSubscription, userIdSubscription;

    public InitialUserProfileView(InitialUserProfileModel model, InitUserProfileController controller) {
        super(new ScrollPane(), model, controller);

        vBox = new VBox();
        root.setContent(vBox);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setFitToWidth(true);

        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(50);
        vBox.getStyleClass().add("content-pane");

        Label headLineLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.headline"));
        headLineLabel.setWrapText(true);
        headLineLabel.setStyle("-fx-text-fill:-bisq-text-color-white;-fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 4em;");
        VBox.setMargin(headLineLabel, new Insets(85, 200, 0, 200));
        VBox.setVgrow(headLineLabel, Priority.ALWAYS);

        Label subTitleLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.subTitle"));
        subTitleLabel.setWrapText(true);
        subTitleLabel.setTextAlignment(TextAlignment.CENTER);
        subTitleLabel.getStyleClass().add("sub-title-label");
        VBox.setMargin(subTitleLabel, new Insets(-24, 200, 0, 200));
        VBox.setVgrow(subTitleLabel, Priority.ALWAYS);

        // nickname
        int width = 520;
        Pane nickNameInput = new Pane();
        nickNameInput.setStyle("-fx-background-color: #2E2E2E; -fx-background-radius: 5");

        Label nickNameLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.nickName").toUpperCase());
        nickNameLabel.setStyle("-fx-text-fill: #737373; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.4em;");
        nickNameLabel.setLayoutY(10);
        nickNameLabel.setLayoutX(14);

        Label nickNameLabelPrompt = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.nickName.prompt"));
        nickNameLabelPrompt.setStyle("-fx-text-fill: #4E4E4E; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.9em;");
        nickNameLabelPrompt.setLayoutY(33);
        nickNameLabelPrompt.setLayoutX(14);
        nickNameLabelPrompt.setCursor(Cursor.TEXT);

        nickNameTextField = new TextField();
        nickNameTextField.setLayoutY(28);
        nickNameTextField.setLayoutX(6);
        nickNameTextField.setMinWidth(width);
        nickNameTextField.setMaxWidth(width);
        nickNameTextField.setStyle("-fx-faint-focus-color: transparent; -fx-font-family: \"IBM Plex Sans Light\"; -fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 1.9em;");
        nickNameTextField.setVisible(false);
        nickNameInput.setOnMousePressed(e -> {
            nickNameTextField.setVisible(true);
            nickNameLabelPrompt.setVisible(false);
            UIThread.runOnNextRenderFrame(() -> nickNameTextField.requestFocus());
        });

        nickNameInput.setMinWidth(width);
        nickNameInput.setMaxWidth(width);
        nickNameInput.setMinHeight(75);
        nickNameInput.setMaxHeight(75);
        nickNameInput.getChildren().addAll(nickNameLabel, nickNameTextField, nickNameLabelPrompt);


        roboIconImageView = new ImageView();
        Tooltip.install(roboIconImageView, new Tooltip(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.button")));
        roboIconImageView.setCursor(Cursor.HAND);

        generatedUserName = new Label();
        generatedUserName.setMaxWidth(300);
        generatedUserName.setMinWidth(300);
        generatedUserName.setTextAlignment(TextAlignment.CENTER);
        generatedUserName.setPadding(new Insets(7, 7, 7, 7));
        generatedUserName.setStyle("-fx-background-color: -bisq-grey-6; -fx-text-fill: white; -fx-font-size: 1.1em");
        VBox.setMargin(generatedUserName, new Insets(-50, 0, 0, 0));

        Label tryOtherInfoLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.info"));
        tryOtherInfoLabel.setTextAlignment(TextAlignment.CENTER);
        tryOtherInfoLabel.getStyleClass().add("sub-title-label");
        VBox.setMargin(tryOtherInfoLabel, new Insets(-10, 0, -10, 0));


        createUserButton = new BisqButton(Res.get("satoshisquareapp.setDefaultUserProfile.done"));
        createUserButton.getStyleClass().add("bisq-button-2");
        //15px
        //  createUserButton.setStyle("-fx-text-fill: white; -fx-background-color: #2E2E2E; -fx-background-radius: 4; -fx-font-size: 1.2em; -fx-padding: 15 47 15 47");

        // createUserButton.getStyleClass().add("action-button");

        processingLabel = new BisqLabel();
        processingLabel.setWrapText(true);

    /*    BisqLabel infoLabel = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.info"));
        infoLabel.setWrapText(true);
        VBox.setMargin(infoLabel, new Insets(50, 0, 0, 0));
        infoLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: -fx-light-text-color;");
      */
        vBox.getChildren().addAll(
                headLineLabel,
                subTitleLabel,
                nickNameInput,
                roboIconImageView,
                generatedUserName,
                tryOtherInfoLabel,
                createUserButton,
                processingLabel
        );
    }

    @Override
    protected void onViewAttached() {
        createUserButton.disableProperty().bind(model.createProfileButtonDisable);
        userIdSubscription = EasyBind.subscribe(model.profileId, userName -> generatedUserName.setText("ID: " + userName));
        processingLabel.textProperty().bind(model.feedback);
        nickNameTextField.textProperty().bindBidirectional(model.nickName);

        roboIconImageView.setOnMousePressed(e -> controller.onCreateTempIdentity());
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
        createUserButton.disableProperty().unbind();
        processingLabel.textProperty().unbind();
        nickNameTextField.textProperty().unbindBidirectional(model.nickName);
        roboIconImageView.setOnMousePressed(null);
        createUserButton.setOnAction(null);

        roboHashNodeSubscription.unsubscribe();
        userIdSubscription.unsubscribe();
    }
}
