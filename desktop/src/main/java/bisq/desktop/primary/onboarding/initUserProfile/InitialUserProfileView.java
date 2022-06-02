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
import bisq.desktop.components.controls.RoboIconWithId;
import bisq.desktop.components.controls.TextInputBox;
import bisq.desktop.popups.Popup;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class InitialUserProfileView extends View<ScrollPane, InitialUserProfileModel, InitUserProfileController> {
    private final Button nextButton;
    private final VBox vBox;
    private final TextInputBox nicknameTextInputBox;
    private final RoboIconWithId roboIconWithId;
    private Subscription roboHashNodeSubscription, vBoxHeightSubscription;
    private Popup processingPopup;

    public InitialUserProfileView(InitialUserProfileModel model, InitUserProfileController controller) {
        super(new ScrollPane(), model, controller);

        vBox = new VBox();
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(30);
        vBox.getStyleClass().add("bisq-content-bg");

        root.setContent(vBox);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setFitToWidth(true);
        // We must set setFitToHeight false as otherwise text wrapping does not work at labels
        // We need to apply prefViewportHeight once we know our vbox height.
        root.setFitToHeight(false);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.headline"));
        headLineLabel.setWrapText(true);
        headLineLabel.getStyleClass().add("bisq-big-light-headline-label");
        VBox.setMargin(headLineLabel, new Insets(50, 200, 0, 200));
        VBox.setVgrow(headLineLabel, Priority.ALWAYS);

        Label subTitleLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.subTitle"));
        subTitleLabel.setWrapText(true);
        subTitleLabel.setTextAlignment(TextAlignment.CENTER);
        int inputWidth = 450;
        subTitleLabel.setMaxWidth(inputWidth);
        subTitleLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        VBox.setMargin(subTitleLabel, new Insets(0, 200, 0, 200));
        VBox.setVgrow(subTitleLabel, Priority.ALWAYS);

        nicknameTextInputBox = new TextInputBox(Res.get("satoshisquareapp.setDefaultUserProfile.nickName"),
                Res.get("satoshisquareapp.setDefaultUserProfile.nickName.prompt"));
        nicknameTextInputBox.setPrefWidth(300);

        roboIconWithId = new RoboIconWithId(300);
        Tooltip.install(roboIconWithId, new Tooltip(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.button")));

        Label tryOtherInfoLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.info"));
        tryOtherInfoLabel.setWrapText(true);
        tryOtherInfoLabel.setMaxWidth(inputWidth);
        tryOtherInfoLabel.setAlignment(Pos.CENTER);
        tryOtherInfoLabel.setTextAlignment(TextAlignment.CENTER);
        tryOtherInfoLabel.getStyleClass().add("bisq-small-light-label-dimmed");
        VBox.setVgrow(tryOtherInfoLabel, Priority.ALWAYS);
        VBox.setMargin(tryOtherInfoLabel, new Insets(0, 0, 0, 0));

        nextButton = new Button(Res.get("shared.nextStep"));
        nextButton.setDefaultButton(true);
        VBox.setMargin(nextButton, new Insets(0, 0, 50, 0));

        vBox.getChildren().addAll(
                headLineLabel,
                subTitleLabel,
                roboIconWithId,
                tryOtherInfoLabel,
                nicknameTextInputBox,
                nextButton
        );
    }

    @Override
    protected void onViewAttached() {
        nextButton.disableProperty().bind(model.createProfileButtonDisable);
        roboIconWithId.textProperty().bind(model.profileId);
        nicknameTextInputBox.textProperty().bindBidirectional(model.nickName);
        nicknameTextInputBox.requestFocus();

        roboIconWithId.setOnAction(controller::onCreateTempIdentity);
        nextButton.setOnAction(e -> controller.onCreateUserProfile());

        // As we must set setFitToHeight false we need to apply prefViewportHeight once we know our vbox height.
        vBoxHeightSubscription = EasyBind.subscribe(vBox.heightProperty(), h -> {
            double height = h.doubleValue();
            if (height > 0) {
                root.setPrefViewportHeight(height);
                UIThread.runOnNextRenderFrame(() -> {
                    vBoxHeightSubscription.unsubscribe();
                    vBoxHeightSubscription = null;
                });
            }
        });
        roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
            if (roboIcon != null) {
                roboIconWithId.setRoboHashImage(roboIcon);
            }
            roboIconWithId.setVisible(roboIcon != null);
        });

        EasyBind.subscribe(model.showProcessingPopup, show -> {
            if (show) {
                processingPopup = new Popup().information(Res.get("social.createUserProfile.prepare"));
                processingPopup.hideCloseButton();
                processingPopup.show();
            } else if (processingPopup != null) {
                processingPopup.hide();
                processingPopup = null;
            }
        });
    }

    @Override
    protected void onViewDetached() {
        nextButton.disableProperty().unbind();
        nicknameTextInputBox.textProperty().unbindBidirectional(model.nickName);
        roboIconWithId.textProperty().unbind();
        roboIconWithId.setOnAction(null);
        nextButton.setOnAction(null);
        if (vBoxHeightSubscription != null) {
            vBoxHeightSubscription.unsubscribe();
        }
        roboHashNodeSubscription.unsubscribe();
    }
}
