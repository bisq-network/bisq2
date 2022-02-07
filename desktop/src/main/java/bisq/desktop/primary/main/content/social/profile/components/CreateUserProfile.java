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
import bisq.social.userprofile.UserProfileService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.KeyPair;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class CreateUserProfile {
    private final Controller controller;

    public CreateUserProfile(UserProfileService userProfileService, KeyPairService keyPairService) {
        controller = new Controller(userProfileService, keyPairService);
    }

    public View getView() {
        return controller.view;
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private final KeyPairService keyPairService;
        private final ChangeListener<String> userNameListener;
        private final EntitlementSelection entitlementSelection;


        private Controller(UserProfileService userProfileService, KeyPairService keyPairService) {
            this.userProfileService = userProfileService;
            this.keyPairService = keyPairService;
            model = new Model();
            entitlementSelection = new EntitlementSelection(userProfileService, model.tempKeyPair);

           
            view = new View(model, this, entitlementSelection.getView());

            userNameListener = (observable, oldValue, newValue) -> onCreateRoboIcon();
        }

        @Override
        public void onViewAttached() {
            if (model.roboHashNode.get() == null) {
                onCreateRoboIcon();
            }
            reset();
            model.feedback.set("");

            model.createProfileButtonDisable.bind(EasyBind.combine(model.userName, model.roboHashNode,
                    (userName, roboHashNode) -> userName == null || userName.isEmpty() || roboHashNode == null));
            model.userName.addListener(userNameListener);
        }

        @Override
        public void onViewDetached() {
            model.createProfileButtonDisable.unbind();
            model.userName.removeListener(userNameListener);
        }

        private void onCreateUserProfile() {
            model.changeRoboIconButtonDisable.set(true);
            model.feedback.set(Res.get("social.createUserProfile.prepare"));
            String useName = model.userName.get();
            userProfileService.createNewInitializedUserProfile(useName, model.tempKeyId, model.tempKeyPair.get(), entitlementSelection.getVerifiedEntitlements())
                    .thenAccept(userProfile -> {
                        UIThread.run(() -> {
                            checkArgument(userProfile.identity().pubKeyHash().equals(new ByteArray(DigestUtil.hash(model.tempKeyPair.get().getPublic().getEncoded()))));
                            checkArgument(userProfile.identity().domainId().equals(useName));
                            reset();
                            model.feedback.set(Res.get("social.createUserProfile.success", useName));
                        });
                    });
        }

        private void onCreateRoboIcon() {
            model.tempKeyId = StringUtils.createUid();
            model.tempKeyPair.set(keyPairService.generateKeyPair());
            model.roboHashNode.set(RoboHash.getImage(new ByteArray(DigestUtil.hash(model.tempKeyPair.get().getPublic().getEncoded())), false));
        }

        private void reset() {
            model.userName.set("");
            model.changeRoboIconButtonDisable.set(false);
            model.tempKeyId = null;
            model.tempKeyPair.set(null);
            model.roboHashNode.set(null);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        final ObjectProperty<KeyPair> tempKeyPair = new SimpleObjectProperty<>();
        final StringProperty feedback = new SimpleStringProperty();
        final StringProperty userName = new SimpleStringProperty();
        final BooleanProperty changeRoboIconButtonDisable = new SimpleBooleanProperty();
        final BooleanProperty createProfileButtonDisable = new SimpleBooleanProperty();
        String tempKeyId;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final BisqButton changeRoboIconButton, createUserButton;
        private final BisqTextField userNameInputField;
        private final BisqLabel feedbackLabel;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller, EntitlementSelection.View entitlementSelectionView) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            BisqLabel headline = new BisqLabel(Res.get("social.createUserProfile.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");
            headline.setPadding(new Insets(0, 0, 10, 0));

            userNameInputField = new BisqTextField();
            userNameInputField.setMinWidth(300);
            userNameInputField.setPromptText(Res.get("social.createUserProfile.userName.prompt"));

            changeRoboIconButton = new BisqButton(Res.get("social.createUserProfile.changeIconButton"));
            changeRoboIconButton.setMinWidth(userNameInputField.getMinWidth());

            createUserButton = new BisqButton(Res.get("social.createUserProfile.createButton"));
            createUserButton.setMinWidth(userNameInputField.getMinWidth());
            createUserButton.disableProperty().bind(userNameInputField.textProperty().isEmpty());
            createUserButton.getStyleClass().add("action-button");

            VBox vBox = new VBox();
            vBox.setSpacing(Layout.SPACING);
            vBox.getChildren().addAll(userNameInputField, changeRoboIconButton);

            HBox hBox = new HBox();
            hBox.setSpacing(Layout.SPACING);
            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(75);
            roboIconImageView.setFitHeight(75);
            hBox.getChildren().addAll(vBox, roboIconImageView);

            feedbackLabel = new BisqLabel();
            feedbackLabel.setWrapText(true);

            VBox entitlementSelectionViewRoot = entitlementSelectionView.getRoot();

            root.getChildren().addAll(headline, hBox, entitlementSelectionViewRoot, createUserButton, feedbackLabel);
        }

        @Override
        public void onViewAttached() {
            createUserButton.disableProperty().bind(model.createProfileButtonDisable);
            changeRoboIconButton.disableProperty().bind(model.changeRoboIconButtonDisable);
            userNameInputField.textProperty().bindBidirectional(model.userName);
            feedbackLabel.textProperty().bind(model.feedback);

            changeRoboIconButton.setOnAction(e -> controller.onCreateRoboIcon());
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
            changeRoboIconButton.disableProperty().unbind();
            userNameInputField.textProperty().unbindBidirectional(model.userName);
            feedbackLabel.textProperty().unbind();

            changeRoboIconButton.setOnAction(null);
            createUserButton.setOnAction(null);

            roboHashNodeSubscription.unsubscribe();
        }
    }
}