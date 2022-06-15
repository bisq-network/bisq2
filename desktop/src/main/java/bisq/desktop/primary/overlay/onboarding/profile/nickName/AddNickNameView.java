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

package bisq.desktop.primary.overlay.onboarding.profile.nickName;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.TextInputBox;
import bisq.desktop.components.robohash.RoboHash;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddNickNameView extends View<VBox, AddNickNameModel, AddNickNameController> {
    private final Button createProfileButton;
    private final TextInputBox nicknameTextInputBox;
    private final ImageView roboIconView;
    private final ProgressIndicator createProfileIndicator;
    private final Label nickName, nymId;

    public AddNickNameView(AddNickNameModel model, AddNickNameController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(8);
        root.setPadding(new Insets(10, 0, 30, 0));

        Label headLineLabel = new Label(Res.get("addNickName.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("addNickName.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(400);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");


        roboIconView = new ImageView();
        roboIconView.setCursor(Cursor.HAND);
        int size = 128;
        roboIconView.setFitWidth(size);
        roboIconView.setFitHeight(size);

        nickName = new Label();
        nickName.setTextAlignment(TextAlignment.CENTER);
        nickName.setAlignment(Pos.CENTER);
        nickName.getStyleClass().addAll("bisq-text-5");

        nymId = new Label();
        nymId.setTextAlignment(TextAlignment.CENTER);
        nymId.setAlignment(Pos.CENTER);
        nymId.getStyleClass().addAll("bisq-text-3");

        HBox displayName = new HBox(3, nickName, nymId);
        displayName.setAlignment(Pos.CENTER);

        nicknameTextInputBox = new TextInputBox(Res.get("addNickName.nickName"),
                Res.get("addNickName.nickName.prompt"));
        nicknameTextInputBox.setPrefWidth(400);


        createProfileButton = new Button(Res.get("addNickName.createProfile"));
        createProfileButton.setGraphicTextGap(8.0);
        createProfileButton.setContentDisplay(ContentDisplay.RIGHT);
        createProfileButton.setDefaultButton(true);

        createProfileIndicator = new ProgressIndicator();
        createProfileIndicator.setProgress(0);
        createProfileIndicator.setMaxWidth(24);
        createProfileIndicator.setMaxHeight(24);
        createProfileIndicator.setManaged(false);
        createProfileIndicator.setVisible(false);
        HBox hBox = new HBox(10, createProfileButton, createProfileIndicator);
        hBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headLineLabel, new Insets(40, 0, 0, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 3, 0));
        VBox.setMargin(nicknameTextInputBox, new Insets(30, 0, 50, 0));

        root.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                roboIconView,
                displayName,
                nicknameTextInputBox,
                hBox
        );
    }

    @Override
    protected void onViewAttached() {
        roboIconView.setImage(RoboHash.getImage(model.getTempIdentity().proofOfWork().getPayload()));
        nickName.textProperty().bind(model.getNickName());
        nymId.textProperty().bind(model.getNymId());
        nicknameTextInputBox.textProperty().bindBidirectional(model.getNickName());
        
        createProfileButton.disableProperty().bind(model.getCreateProfileButtonDisabled());
        createProfileIndicator.managedProperty().bind(model.getCreateProfileProgress().lessThan(0));
        createProfileIndicator.visibleProperty().bind(model.getCreateProfileProgress().lessThan(0));
        createProfileIndicator.progressProperty().bind(model.getCreateProfileProgress());
        createProfileButton.mouseTransparentProperty().bind(model.getCreateProfileButtonDisabled());

        createProfileButton.setOnAction(e -> controller.onCreateUserProfile());

        nicknameTextInputBox.requestFocus();
    }

    @Override
    protected void onViewDetached() {
        roboIconView.setImage(null);
        nickName.textProperty().unbind();
        nymId.textProperty().unbind();
        nicknameTextInputBox.textProperty().unbindBidirectional(model.getNickName());
        
        createProfileButton.disableProperty().unbind();
        createProfileIndicator.managedProperty().unbind();
        createProfileIndicator.visibleProperty().unbind();
        createProfileIndicator.progressProperty().unbind();
        createProfileButton.mouseTransparentProperty().unbind();
        
        createProfileButton.setOnAction(null);
    }
}
