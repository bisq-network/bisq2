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
import bisq.desktop.components.controls.LargeRoboIconWithId;
import bisq.desktop.components.controls.TextInputBox;
import bisq.desktop.overlay.Popup;
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
    private final Button createUserButton;
    private final VBox vBox;
    private final TextInputBox nicknameTextInputBox;
    private final LargeRoboIconWithId roboIconWithId;
    private Subscription roboHashNodeSubscription, vBoxHeightSubscription;
    private Popup processingPopup;

    public InitialUserProfileView(InitialUserProfileModel model, InitUserProfileController controller) {
        super(new ScrollPane(), model, controller);

        vBox = new VBox();
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setSpacing(50);
        vBox.getStyleClass().add("content-pane");

        root.setContent(vBox);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setFitToWidth(true);
        // We must set setFitToHeight false as otherwise text wrapping does not work at labels
        // We need to apply prefViewportHeight once we know our vbox height.
        root.setFitToHeight(false);
        root.getStyleClass().add("content-pane");

        Label headLineLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.headline"));
        headLineLabel.setWrapText(true);
        headLineLabel.getStyleClass().add("bisq-big-light-headline-label");
        VBox.setMargin(headLineLabel, new Insets(48, 200, 0, 200));
        VBox.setVgrow(headLineLabel, Priority.ALWAYS);

        Label subTitleLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.subTitle"));
        subTitleLabel.setWrapText(true);
        subTitleLabel.setTextAlignment(TextAlignment.CENTER);
        subTitleLabel.getStyleClass().add("bisq-sub-title-label");
        VBox.setMargin(subTitleLabel, new Insets(-33, 200, 0, 200));
        VBox.setVgrow(subTitleLabel, Priority.ALWAYS);

        nicknameTextInputBox = new TextInputBox(Res.get("satoshisquareapp.setDefaultUserProfile.nickName"),
                Res.get("satoshisquareapp.setDefaultUserProfile.nickName.prompt"));
        nicknameTextInputBox.setPrefWidth(520);

        roboIconWithId = new LargeRoboIconWithId();
        Tooltip.install(roboIconWithId, new Tooltip(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.button")));

        Label tryOtherInfoLabel = new Label(Res.get("satoshisquareapp.setDefaultUserProfile.tryOther.info"));
        tryOtherInfoLabel.setWrapText(true);
        tryOtherInfoLabel.setTextAlignment(TextAlignment.CENTER);
        tryOtherInfoLabel.getStyleClass().add("bisq-sub-title-label");
        VBox.setVgrow(tryOtherInfoLabel, Priority.ALWAYS);
        VBox.setMargin(tryOtherInfoLabel, new Insets(-10, 0, -10, 0));

        createUserButton = new Button(Res.get("satoshisquareapp.setDefaultUserProfile.done"));
        createUserButton.getStyleClass().add("bisq-button-2");
        VBox.setMargin(createUserButton, new Insets(0, 0, 100, 0));

        vBox.getChildren().addAll(
                headLineLabel,
                subTitleLabel,
                nicknameTextInputBox,
                roboIconWithId,
                tryOtherInfoLabel,
                createUserButton
        );
    }

    @Override
    protected void onViewAttached() {
        createUserButton.disableProperty().bind(model.createProfileButtonDisable);
        roboIconWithId.textProperty().bind(model.profileId);
        nicknameTextInputBox.textProperty().bindBidirectional(model.nickName);

        roboIconWithId.setOnAction(controller::onCreateTempIdentity);
        createUserButton.setOnAction(e -> controller.onCreateUserProfile());

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
                roboIconWithId.setImage(roboIcon);
            }
            roboIconWithId.setVisible(roboIcon != null);
        });

        EasyBind.subscribe(model.showProcessingPopup, show -> {
            if (show) {
                processingPopup = new Popup().information(Res.get("social.createUserProfile.prepare"));
                processingPopup.show();
            } else if (processingPopup != null) {
                processingPopup.hide();
                processingPopup = null;
            }
        });
    }

    @Override
    protected void onViewDetached() {
        createUserButton.disableProperty().unbind();
        nicknameTextInputBox.textProperty().unbindBidirectional(model.nickName);
        roboIconWithId.textProperty().unbind();
        roboIconWithId.setOnAction(null);
        createUserButton.setOnAction(null);
        if (vBoxHeightSubscription != null) {
            vBoxHeightSubscription.unsubscribe();
        }
        roboHashNodeSubscription.unsubscribe();
    }
}
