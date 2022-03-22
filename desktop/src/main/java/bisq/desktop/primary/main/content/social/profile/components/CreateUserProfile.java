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

package bisq.desktop.primary.main.content.social.profile.components;

import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqTextField;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.social.user.UserNameGenerator;
import bisq.social.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.KeyPair;
import java.util.HashSet;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class CreateUserProfile {
    private final Controller controller;

    public CreateUserProfile(UserProfileService userProfileService, KeyPairService keyPairService) {
        controller = new Controller(userProfileService, keyPairService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private final KeyPairService keyPairService;
        private final EntitlementSelection entitlementSelection;


        private Controller(UserProfileService userProfileService, KeyPairService keyPairService) {
            this.userProfileService = userProfileService;
            this.keyPairService = keyPairService;
            model = new Model();
            entitlementSelection = new EntitlementSelection(userProfileService);

            view = new View(model, this, entitlementSelection.getRoot());
        }

        @Override
        public void onViewAttached() {
            reset();
            model.feedback.set("");
            onCreateIdentity();

            model.createProfileButtonDisable.bind(EasyBind.combine(model.userName, model.roboHashNode,
                    (userName, roboHashNode) -> userName == null || userName.isEmpty() || roboHashNode == null));
        }

        @Override
        public void onViewDetached() {
            model.createProfileButtonDisable.unbind();
        }

        private void onCreateUserProfile() {
            model.generateNewIdentityButtonDisable.set(true);
            model.feedback.set(Res.get("social.createUserProfile.prepare"));
            String useName = model.userName.get();
            userProfileService.createNewInitializedUserProfile(useName,
                            model.tempKeyId,
                            model.tempKeyPair,
                            new HashSet<>(entitlementSelection.getVerifiedEntitlements()))
                    .thenAccept(userProfile -> {
                        UIThread.run(() -> {
                            checkArgument(userProfile.identity().pubKeyHashAsByteArray().equals(new ByteArray(DigestUtil.hash(model.tempKeyPair.getPublic().getEncoded()))));
                            checkArgument(userProfile.identity().domainId().equals(useName));
                            reset();
                            model.feedback.set(Res.get("social.createUserProfile.success", useName));
                        });
                    });
        }

        private void onCreateIdentity() {
            model.tempKeyId = StringUtils.createUid();
            model.tempKeyPair = keyPairService.generateKeyPair();
            model.roboHashNode.set(RoboHash.getImage(new ByteArray(DigestUtil.hash(model.tempKeyPair.getPublic().getEncoded())), false));
            model.userName.set(UserNameGenerator.fromHash(model.tempKeyPair.getPublic().getEncoded()));
        }

        private void reset() {
            model.userName.set("");
            model.generateNewIdentityButtonDisable.set(false);
            model.entitlementSelectionVisible.set(false);
            model.tempKeyId = null;
            model.tempKeyPair = null;
            model.roboHashNode.set(null);
            entitlementSelection.reset();
        }

        private void onShowEntitlementSelection() {
            entitlementSelection.show(model.tempKeyPair);
            model.entitlementSelectionVisible.set(true);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        final StringProperty feedback = new SimpleStringProperty();
        final StringProperty userName = new SimpleStringProperty();
        final BooleanProperty generateNewIdentityButtonDisable = new SimpleBooleanProperty();
        final BooleanProperty createProfileButtonDisable = new SimpleBooleanProperty();
        private final BooleanProperty entitlementSelectionVisible = new SimpleBooleanProperty();

        KeyPair tempKeyPair = null;
        String tempKeyId;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final BisqButton generateNewIdentityButton, entitlementButton, createUserButton;
        private final BisqTextField userNameInputField;
        private final BisqLabel feedbackLabel;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller, Pane entitlementSelection) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            BisqLabel headline = new BisqLabel(Res.get("social.createUserProfile.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");
            headline.setPadding(new Insets(0, 0, 10, 0));

            userNameInputField = new BisqTextField();
            double minWidth = 300;
            userNameInputField.setMinWidth(minWidth);
            userNameInputField.setEditable(false);
            userNameInputField.setFocusTraversable(false);
            userNameInputField.setPromptText(Res.get("social.createUserProfile.userName.prompt"));

            generateNewIdentityButton = new BisqButton(Res.get("social.createUserProfile.generateNewIdentity"));
            generateNewIdentityButton.setMinWidth(minWidth);

            entitlementButton = new BisqButton(Res.get("social.createUserProfile.entitlement.headline"));
            entitlementButton.setMinWidth(minWidth);

            createUserButton = new BisqButton(Res.get("social.createUserProfile.createButton"));
            createUserButton.setMinWidth(minWidth);
            createUserButton.disableProperty().bind(userNameInputField.textProperty().isEmpty());
            createUserButton.getStyleClass().add("action-button");

            VBox vBox = new VBox();
            vBox.setSpacing(Layout.SPACING);
            vBox.getChildren().addAll(userNameInputField, generateNewIdentityButton, entitlementButton);

            HBox hBox = new HBox();
            hBox.setSpacing(Layout.SPACING);
            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(75);
            roboIconImageView.setFitHeight(75);
            hBox.getChildren().addAll(vBox, roboIconImageView);

            feedbackLabel = new BisqLabel();
            feedbackLabel.setWrapText(true);

            root.getChildren().addAll(headline, hBox, entitlementSelection, createUserButton, feedbackLabel);
        }

        @Override
        public void onViewAttached() {
            generateNewIdentityButton.disableProperty().bind(model.generateNewIdentityButtonDisable);
            entitlementButton.setOnAction(e -> controller.onShowEntitlementSelection());
            entitlementButton.visibleProperty().bind(model.entitlementSelectionVisible.not());
            entitlementButton.managedProperty().bind(model.entitlementSelectionVisible.not());
            createUserButton.disableProperty().bind(model.createProfileButtonDisable);
            userNameInputField.textProperty().bindBidirectional(model.userName);
            feedbackLabel.textProperty().bind(model.feedback);

            generateNewIdentityButton.setOnAction(e -> controller.onCreateIdentity());
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
            generateNewIdentityButton.disableProperty().unbind();
            entitlementButton.setOnAction(null);
            entitlementButton.visibleProperty().unbind();
            entitlementButton.managedProperty().unbind();
            createUserButton.disableProperty().unbind();
            userNameInputField.textProperty().unbindBidirectional(model.userName);
            feedbackLabel.textProperty().unbind();

            generateNewIdentityButton.setOnAction(null);
            createUserButton.setOnAction(null);

            roboHashNodeSubscription.unsubscribe();
        }
    }
}
