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

package bisq.desktop.primary.main.content.social.profile;

import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.controls.BisqTextField;
import bisq.desktop.layout.Layout;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.social.userprofile.UserProfileService;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
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

public class CreateUserProfile {
    private final Controller controller;

    public CreateUserProfile(UserProfileService userProfileService, KeyPairService keyPairService) {
        controller = new Controller(userProfileService, keyPairService);
    }

    public View getView() {
        return controller.view;
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserProfileService userProfileService;
        private final KeyPairService keyPairService;
        private final ChangeListener<String> userNameListener;

        private Controller(UserProfileService userProfileService, KeyPairService keyPairService) {
            this.userProfileService = userProfileService;
            this.keyPairService = keyPairService;

            model = new Model();
            view = new View(model, this);

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
            model.feedback.set(Res.common.get("social.createUserProfile.prepare"));
            String useName = model.userName.get();
            userProfileService.createNewInitializedUserProfile(useName, model.ephemeralKeyId, model.ephemeralKeyPair)
                    .thenAccept(userProfile -> {
                        UIThread.run(() -> {
                            checkArgument(userProfile.identity().pubKeyHash().equals(new ByteArray(DigestUtil.hash(model.ephemeralKeyPair.getPublic().getEncoded()))));
                            checkArgument(userProfile.identity().domainId().equals(useName));
                            reset();
                            model.feedback.set(Res.common.get("social.createUserProfile.success", useName));
                        });
                    });
        }

        private void reset() {
            model.userName.set("");
            model.changeRoboIconButtonDisable.set(false);
            model.ephemeralKeyId = null;
            model.ephemeralKeyPair = null;
            model.roboHashNode.set(null);
        }

        private void onCreateRoboIcon() {
            model.ephemeralKeyId = StringUtils.createUid();
            model.ephemeralKeyPair = keyPairService.generateKeyPair();
            model.roboHashNode.set(RoboHash.getImage(new ByteArray(DigestUtil.hash(model.ephemeralKeyPair.getPublic().getEncoded())), false));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        String ephemeralKeyId;
        ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        StringProperty feedback = new SimpleStringProperty();
        StringProperty userName = new SimpleStringProperty();
        BooleanProperty changeRoboIconButtonDisable = new SimpleBooleanProperty();
        BooleanProperty createProfileButtonDisable = new SimpleBooleanProperty();
        KeyPair ephemeralKeyPair;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final BisqButton changeRoboIconButton, createUserButton;
        private final BisqTextField userNameInputField;
        private final BisqLabel feedbackLabel;
        private Subscription roboHashNodeSubscription, userProfileSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new BisqLabel(Res.common.get("social.createUserProfile.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            userNameInputField = new BisqTextField();
            userNameInputField.setPromptText(Res.common.get("social.createUserProfile.userName.prompt"));

            changeRoboIconButton = new BisqButton(Res.common.get("social.createUserProfile.changeIconButton"));

            createUserButton = new BisqButton(Res.common.get("social.createUserProfile.createButton"));
            createUserButton.disableProperty().bind(userNameInputField.textProperty().isEmpty());

            VBox vBox = new VBox();
            vBox.setSpacing(Layout.SPACING);
            vBox.getChildren().addAll(userNameInputField, changeRoboIconButton, createUserButton);

            HBox hBox = new HBox();
            hBox.setSpacing(Layout.SPACING);
            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(75);
            roboIconImageView.setFitHeight(75);
            hBox.getChildren().addAll(vBox, roboIconImageView);

            feedbackLabel = new BisqLabel();
            feedbackLabel.setWrapText(true);

            root.getChildren().addAll(headline, hBox, feedbackLabel);
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