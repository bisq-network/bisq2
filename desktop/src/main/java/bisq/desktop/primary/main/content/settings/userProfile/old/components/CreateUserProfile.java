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

package bisq.desktop.primary.main.content.settings.userProfile.old.components;

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.security.DigestUtil;
import bisq.security.KeyPairService;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.ProofOfWorkService;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUserService;
import bisq.social.user.NymIdGenerator;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class CreateUserProfile {
    private final Controller controller;

    public CreateUserProfile(ChatService chatService, ChatUserService chatUserService, SecurityService securityService) {
        controller = new Controller(chatService, chatUserService, securityService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatUserService chatUserService;
        private final KeyPairService keyPairService;
        private final RoleSelection roleSelection;
        private final ProofOfWorkService proofOfWorkService;
        private Optional<CompletableFuture<Void>> mintNymProofOfWorkFuture = Optional.empty();

        private Controller(ChatService chatService, ChatUserService chatUserService, SecurityService securityService) {
            this.chatUserService = chatUserService;
            keyPairService = securityService.getKeyPairService();
            proofOfWorkService = securityService.getProofOfWorkService();
            model = new Model();
            roleSelection = new RoleSelection(chatUserService);

            view = new View(model, this, roleSelection.getRoot());
        }

        @Override
        public void onActivate() {
            reset();
            model.feedback.set("");
            onCreateIdentity();

            model.createProfileButtonDisable.bind(EasyBind.combine(model.nickName, model.nymId, model.roboHashImage,
                    (nickName, profileId, roboHashNode) -> nickName == null || nickName.isEmpty() ||
                            profileId == null || profileId.isEmpty() ||
                            roboHashNode == null));
        }

        @Override
        public void onDeactivate() {
            model.createProfileButtonDisable.unbind();

            // Does only cancel downstream calls not actual running task
            // We pass the isCanceled flag to stop the running task
            mintNymProofOfWorkFuture.ifPresent(future -> future.cancel(true));
        }

        private void onCreateUserProfile() {
            model.generateNewIdentityButtonDisable.set(true);
            model.feedback.set(Res.get("social.createUserProfile.prepare"));
            String nymId = model.nymId.get();
            chatUserService.createAndPublishNewChatUserIdentity(nymId,
                            model.nickName.get(),
                            model.tempKeyId,
                            model.tempKeyPair,
                            model.proofOfWork,
                            "",
                            "")
                    .thenAccept(userProfile -> UIThread.run(this::reset));
        }

        private void onCreateIdentity() {
            KeyPair tempKeyPair = keyPairService.generateKeyPair();
            byte[] pubKeyHash = DigestUtil.hash(tempKeyPair.getPublic().getEncoded());
            model.roboHashImage.set(null);
            model.roboHashIconVisible.set(false);
            model.createProfileButtonDisable.set(true);
            model.powProgress.set(-1);
            model.nymId.set(Res.get("createProfile.nymId.generating"));
            long ts = System.currentTimeMillis();
            mintNymProofOfWorkFuture = Optional.of(proofOfWorkService.mintNymProofOfWork(pubKeyHash)
                    .thenAccept(proofOfWork -> {
                        UIThread.run(() -> {
                            log.info("Proof of work creation completed after {} ms", System.currentTimeMillis() - ts);
                            model.proofOfWork = proofOfWork;
                            model.tempKeyId = StringUtils.createUid();
                            model.tempKeyPair = tempKeyPair;

                            model.roboHashImage.set(RoboHash.getImage(proofOfWork.getPayload()));
                            model.nymId.set(NymIdGenerator.fromHash(proofOfWork.getPayload()));

                            model.powProgress.set(0);
                            model.roboHashIconVisible.set(true);
                            //model.createProfileButtonDisable.set(model.nickName.get() == null || model.nickName.get().isEmpty());
                        });
                    }));
        }

        private void reset() {
            model.nickName.set("");
            model.nymId.set("");
            model.generateNewIdentityButtonDisable.set(false);
            model.entitlementSelectionVisible.set(false);
            model.tempKeyId = null;
            model.tempKeyPair = null;
            model.roboHashImage.set(null);
            roleSelection.reset();
        }

        private void onShowEntitlementSelection() {
            roleSelection.show(model.tempKeyPair);
            model.entitlementSelectionVisible.set(true);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        final ObjectProperty<Image> roboHashImage = new SimpleObjectProperty<>();
        final StringProperty feedback = new SimpleStringProperty();
        final StringProperty nickName = new SimpleStringProperty();
        final StringProperty nymId = new SimpleStringProperty();
        final BooleanProperty generateNewIdentityButtonDisable = new SimpleBooleanProperty();
        final BooleanProperty createProfileButtonDisable = new SimpleBooleanProperty();
        final BooleanProperty entitlementSelectionVisible = new SimpleBooleanProperty();
        final BooleanProperty roboHashIconVisible = new SimpleBooleanProperty();
        final DoubleProperty powProgress = new SimpleDoubleProperty();
        ProofOfWork proofOfWork;
        KeyPair tempKeyPair = null;
        String tempKeyId;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Button generateNewIdentityButton, entitlementButton, createUserButton;
        private final TextField nickNameInputField, profileIdInputField;
        private final Label feedbackLabel;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller, Pane entitlementSelection) {
            super(new VBox(), model, controller);
            root.setSpacing(10);

            Label headline = new Label(Res.get("social.createUserProfile.headline"));
            headline.getStyleClass().add("titled-group-bg-label-active");
            headline.setPadding(new Insets(0, 0, 10, 0));

            nickNameInputField = new TextField();
            double minWidth = 300;
            nickNameInputField.setMinWidth(minWidth);
            nickNameInputField.setFocusTraversable(false);
            nickNameInputField.setPromptText(Res.get("social.createUserProfile.nickName.prompt"));

            profileIdInputField = new TextField();
            profileIdInputField.setMinWidth(minWidth);
            profileIdInputField.setEditable(false);
            profileIdInputField.setFocusTraversable(false);
            profileIdInputField.setPromptText(Res.get("social.createUserProfile.profileId.prompt"));

            generateNewIdentityButton = new Button(Res.get("social.createUserProfile.generateNewIdentity"));
            generateNewIdentityButton.setMinWidth(minWidth);

            entitlementButton = new Button(Res.get("social.createUserProfile.entitlement.headline"));
            entitlementButton.setMinWidth(minWidth);

            createUserButton = new Button(Res.get("social.createUserProfile.createButton"));
            createUserButton.setMinWidth(minWidth);
            createUserButton.disableProperty().bind(profileIdInputField.textProperty().isEmpty());
            createUserButton.setDefaultButton(true);
            VBox vBox = new VBox();
            vBox.setSpacing(Layout.SPACING);
            vBox.getChildren().addAll(nickNameInputField, profileIdInputField, generateNewIdentityButton, entitlementButton);

            HBox hBox = new HBox();
            hBox.setSpacing(Layout.SPACING);
            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(75);
            roboIconImageView.setFitHeight(75);
            hBox.getChildren().addAll(vBox, roboIconImageView);

            feedbackLabel = new Label();
            feedbackLabel.setWrapText(true);

            root.getChildren().addAll(headline, hBox, entitlementSelection, createUserButton, feedbackLabel);
        }

        @Override
        protected void onViewAttached() {
            generateNewIdentityButton.disableProperty().bind(model.generateNewIdentityButtonDisable);
            entitlementButton.setOnAction(e -> controller.onShowEntitlementSelection());
            entitlementButton.visibleProperty().bind(model.entitlementSelectionVisible.not());
            entitlementButton.managedProperty().bind(model.entitlementSelectionVisible.not());
            createUserButton.disableProperty().bind(model.createProfileButtonDisable);
            nickNameInputField.textProperty().bindBidirectional(model.nickName);
            profileIdInputField.textProperty().bindBidirectional(model.nymId);
            feedbackLabel.textProperty().bind(model.feedback);

            generateNewIdentityButton.setOnAction(e -> controller.onCreateIdentity());
            createUserButton.setOnAction(e -> controller.onCreateUserProfile());

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashImage, roboIcon -> {
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
            nickNameInputField.textProperty().unbindBidirectional(model.nickName);
            profileIdInputField.textProperty().unbindBidirectional(model.nymId);
            feedbackLabel.textProperty().unbind();

            generateNewIdentityButton.setOnAction(null);
            createUserButton.setOnAction(null);

            roboHashNodeSubscription.unsubscribe();
        }
    }
}
