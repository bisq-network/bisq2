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

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.SectionBox;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class InitialUserProfileView extends View<HBox, InitialUserProfileModel, InitUserProfileController> {
    private final ImageView roboIconImageView;
    private final BisqButton createUserButton;
    private final BisqLabel feedbackLabel;
    private final Label userName;
    private Subscription roboHashNodeSubscription;

    public InitialUserProfileView(InitialUserProfileModel model, InitUserProfileController controller) {
        super(new HBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("content-pane");
        root.setFillHeight(false);

        roboIconImageView = new ImageView();
        Tooltip.install(roboIconImageView, new Tooltip(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.button")));
        roboIconImageView.setCursor(Cursor.HAND);
        VBox.setMargin(roboIconImageView, new Insets(10, 0, 0, 0));


        Label userNameLabel = new Label(Res.get("social.createUserProfile.userName.prompt"));
        userNameLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: -fx-light-text-color;");

        userName = new Label();
        userName.setStyle("-fx-background-color: -bs-background-color;-fx-text-fill: -fx-dark-text-color;");
        userName.setMaxWidth(300);
        userName.setMinWidth(300);
        userName.setPadding(new Insets(7, 7, 7, 7));

        VBox userNameBox = new VBox();
        userNameBox.setSpacing(2);
        userNameBox.getChildren().addAll(userNameLabel, userName);
        VBox.setMargin(userNameBox, new Insets(-20, 0, 0, 0));

        BisqLabel tryOtherInfoLabel = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.info"));
        VBox.setMargin(tryOtherInfoLabel, new Insets(-10, 0, -10, 0));

        createUserButton = new BisqButton(Res.get("satoshisquareapp.setDefaultUserProfile.done"));
        createUserButton.disableProperty().bind(userName.textProperty().isEmpty());
        createUserButton.getStyleClass().add("action-button");

        feedbackLabel = new BisqLabel();
        feedbackLabel.setWrapText(true);

        BisqLabel infoLabel = new BisqLabel(Res.get("satoshisquareapp.setDefaultUserProfile.info"));
        infoLabel.setWrapText(true);
        VBox.setMargin(infoLabel, new Insets(50, 0, 0, 0));
        infoLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: -fx-light-text-color;");
      
        SectionBox sectionBox = new SectionBox(Res.get("satoshisquareapp.setDefaultUserProfile.headline"));
        sectionBox.setPrefWidth(450);
        sectionBox.getChildren().addAll(
                roboIconImageView,
                userNameBox,
                tryOtherInfoLabel,
                createUserButton,
                feedbackLabel,
                infoLabel
        );
        root.getChildren().addAll(sectionBox);
    }

    @Override
    protected void onViewAttached() {
        createUserButton.disableProperty().bind(model.createProfileButtonDisable);
        userName.textProperty().bindBidirectional(model.userName);
        feedbackLabel.textProperty().bind(model.feedback);

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
        userName.textProperty().unbindBidirectional(model.userName);
        feedbackLabel.textProperty().unbind();

        roboIconImageView.setOnMousePressed(null);
        createUserButton.setOnAction(null);

        roboHashNodeSubscription.unsubscribe();
    }
}
