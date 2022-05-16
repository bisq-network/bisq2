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

package bisq.desktop.primary.main.content.newProfilePopup.initNymProfile;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class InitNymProfileView extends View<ScrollPane, InitNymProfileModel, InitNymProfileController> {
    private final Button regenerateButton;
    private final Button nextButton;
    private final Label hashIdValue, profileIdValue;
    private final ImageView roboIconView;
    private Subscription roboHashNodeSubscription;

    public InitNymProfileView(InitNymProfileModel model, InitNymProfileController controller) {
        super(new ScrollPane(), model, controller);

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(8);
        vBox.getStyleClass().add("bisq-content-bg");
        vBox.setPadding(new Insets(30, 0, 30, 0));

        root.setContent(vBox);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setFitToWidth(true);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("initNymProfile.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("initNymProfile.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setMaxWidth(280);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 8, 0));

        roboIconView = new ImageView();
        roboIconView.setCursor(Cursor.HAND);
        roboIconView.setFitWidth(128);
        roboIconView.setFitHeight(128);
        
        VBox hashIdBox = getValueBox(Res.get("initNymProfile.uniqueHashId"), "bisq-text-white");
        VBox.setMargin(hashIdBox, new Insets(0, 0, 16, 0));
        hashIdValue = (Label) hashIdBox.getChildren().get(1);

        VBox profileIdBox = getValueBox(Res.get("initNymProfile.customProfileNickname"), "bisq-text-1");
        VBox.setMargin(profileIdBox, new Insets(0, 0, 16, 0));
        profileIdValue = (Label) profileIdBox.getChildren().get(1);
        
        regenerateButton = new Button(Res.get("initNymProfile.regenerate"));
        regenerateButton.getStyleClass().setAll("bisq-transparent-button", "bisq-text-3", "text-underline");
        VBox.setMargin(regenerateButton, new Insets(0, 0, 16, 0));

        nextButton = new Button(Res.get("initNymProfile.createProfile"));
        nextButton.setGraphicTextGap(8.0);
        nextButton.setContentDisplay(ContentDisplay.RIGHT);
        nextButton.setDefaultButton(true);

        vBox.getChildren().addAll(
                headLineLabel,
                subtitleLabel,
                roboIconView,
                hashIdBox,
                profileIdBox,
                regenerateButton,
                nextButton
        );
    }

    @Override
    protected void onViewAttached() {
        hashIdValue.textProperty().bind(model.uniqueHashId);
        profileIdValue.textProperty().bind(model.profileId);
        nextButton.graphicProperty().bind(Bindings.createObjectBinding(() -> {
            if (!model.createProfileInProgress.get()) {
                return null;
            }
            final ProgressIndicator pin = new ProgressIndicator();
            pin.setProgress(-1f);
            pin.setMaxWidth(24.0);
            pin.setMaxHeight(24.0);
            return pin;
        }, model.createProfileInProgress));
        nextButton.disableProperty().bind(model.createProfileInProgress);
        
        regenerateButton.setOnAction(e -> controller.createTempIdentity());
        nextButton.setOnAction(e -> controller.createNymProfile());
        roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIconView::setImage);
    }

    @Override
    protected void onViewDetached() {
        hashIdValue.textProperty().unbind();
        nextButton.graphicProperty().unbind();
        nextButton.disableProperty().unbind();
        roboIconView.setOnMousePressed(null);
        nextButton.setOnAction(null);
        roboHashNodeSubscription.unsubscribe();
    }
    
    private VBox getValueBox(String title, String valueClass) {
        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-4");

        Label valueLabel = new Label();
        valueLabel.getStyleClass().add(valueClass);
        
        VBox box = new VBox(titleLabel, valueLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
