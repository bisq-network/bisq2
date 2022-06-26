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

package bisq.desktop.primary.main.content.settings.userProfile;

import bisq.common.data.Pair;
import bisq.common.data.Triple;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class EditUserProfile {
    private final Controller controller;

    public EditUserProfile(ChatUserService chatUserService, ChatUserIdentity chatUserIdentity) {
        controller = new Controller(chatUserService, chatUserIdentity);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatUserService chatUserService;
        private final Model model;
        @Getter
        private final View view;


        private Controller(ChatUserService chatUserService, ChatUserIdentity chatUserIdentity) {
            this.chatUserService = chatUserService;
            model = new Model(chatUserIdentity);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            ChatUser chatUser = model.chatUserIdentity.getChatUser();
            if (chatUser == null) {
                return;
            }

            model.id.set(Res.get("social.createUserProfile.id", chatUser.getId()));
            model.bio.set(chatUser.getBio());
            model.terms.set(chatUser.getTerms());
            model.reputationScore.set(chatUser.getBurnScoreAsString());
            model.profileAge.set(chatUser.getAccountAgeAsString());

            model.nym.set(chatUser.getNym());
            model.nickName.set(chatUser.getNickName());
            model.roboHashNode.set(RoboHash.getImage(chatUser.getProofOfWork().getPayload()));
        }

        @Override
        public void onDeactivate() {
        }

        public void onEdit() {
            model.isEditMode.set(true);
        }

        public void onCancelEdit() {
            model.isEditMode.set(false);
        }

        public void onSave(String terms, String bio) {
            model.isPublishing.set(true);
            model.progress.set(-1);
            chatUserService.editChatUser(model.chatUserIdentity, terms, bio)
                    .whenComplete((r, t) -> {
                        log.error("{}", r.toString());
                        model.progress.set(0);
                        model.isPublishing.set(false);
                        model.isEditMode.set(false);
                    });
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatUserIdentity chatUserIdentity;
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty bio = new SimpleStringProperty();
        private final StringProperty terms = new SimpleStringProperty();
        private final StringProperty reputationScore = new SimpleStringProperty();
        private final StringProperty profileAge = new SimpleStringProperty();
        private final BooleanProperty isEditMode = new SimpleBooleanProperty();
        private final BooleanProperty isPublishing = new SimpleBooleanProperty();
        private final IntegerProperty progress = new SimpleIntegerProperty();

        private Model(ChatUserIdentity chatUserIdentity) {
            this.chatUserIdentity = chatUserIdentity;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nym, nickName, bio, reputationScore, profileAge, terms;
        private final BisqTextArea bioTextArea, termsTextArea;
        private final Button editButton, saveButton, cancelEditButton;
        private final HBox buttonBar;
        private final ProgressIndicator progressIndicator;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setAlignment(Pos.TOP_LEFT);

            Label headlineLabel = new Label(Res.get("settings.userProfile.selectedProfile").toUpperCase());
            headlineLabel.getStyleClass().add("bisq-text-4");

            nickName = new Label();
            nickName.getStyleClass().addAll("bisq-text-9", "font-semi-bold");
            nickName.setAlignment(Pos.CENTER);

            roboIconImageView = new ImageView();
            roboIconImageView.setFitWidth(100);
            roboIconImageView.setFitHeight(100);

            nym = new Label();
            nym.getStyleClass().addAll("bisq-text-7");
            nym.setAlignment(Pos.CENTER);

            VBox nameAndIconBox = new VBox(10, nickName, roboIconImageView, nym);
            nameAndIconBox.setAlignment(Pos.TOP_CENTER);

            Triple<VBox, Label, BisqTextArea> bioBox = getEditableInfoBox(Res.get("social.chatUser.bio"));
            bio = bioBox.second();
            bioTextArea = bioBox.third();

            Pair<VBox, Label> reputationScoreBox = getInfoBox(Res.get("social.chatUser.reputationScore"));
            reputationScore = reputationScoreBox.second();

            Pair<VBox, Label> profileAgeBox = getInfoBox(Res.get("social.chatUser.profileAge"));
            profileAge = profileAgeBox.second();

            Triple<VBox, Label, BisqTextArea> termsBox = getEditableInfoBox(Res.get("social.chat.chatRules.headline"));
            terms = termsBox.second();
            terms.setText(Res.get("social.chat.chatRules.content")); //todo
            termsTextArea = termsBox.third();

            editButton = new Button(Res.get("edit"));

            saveButton = new Button(Res.get("save"));
            saveButton.setDefaultButton(true);

            cancelEditButton = new Button(Res.get("cancel"));

            progressIndicator = new ProgressIndicator(0);
            progressIndicator.setManaged(false);
            progressIndicator.setVisible(false);
            buttonBar = new HBox(10, cancelEditButton, saveButton, progressIndicator, Spacer.fillHBox());
            buttonBar.setAlignment(Pos.CENTER_LEFT);
            buttonBar.setManaged(false);
            buttonBar.setVisible(false);


            VBox.setMargin(nameAndIconBox, new Insets(0, 0, 20, 0));
            VBox.setMargin(editButton, new Insets(20, 0, 0, 0));
            VBox.setMargin(buttonBar, new Insets(20, 0, 0, 0));
            VBox mainVBox = new VBox(10, headlineLabel, nameAndIconBox,
                    bioBox.first(), reputationScoreBox.first(), profileAgeBox.first(), termsBox.first(), editButton, buttonBar);
            mainVBox.getStyleClass().add("bisq-box-2");
            mainVBox.setPadding(new Insets(30));

            VBox.setMargin(headlineLabel, new Insets(0, 0, 0, 0));

            root.getChildren().addAll(headlineLabel, mainVBox);
        }

        @Override
        protected void onViewAttached() {
            nym.textProperty().bind(model.nym);
            nickName.textProperty().bind(model.nickName);
            bio.textProperty().bind(model.bio);
            terms.textProperty().bind(model.terms);
            bioTextArea.textProperty().bindBidirectional(model.bio);
            termsTextArea.textProperty().bindBidirectional(model.terms);
            reputationScore.textProperty().bind(model.reputationScore);
            profileAge.textProperty().bind(model.profileAge);

            bio.managedProperty().bind(model.isEditMode.not());
            bio.visibleProperty().bind(model.isEditMode.not());
            terms.managedProperty().bind(model.isEditMode.not());
            terms.visibleProperty().bind(model.isEditMode.not());

            bioTextArea.managedProperty().bind(model.isEditMode);
            bioTextArea.visibleProperty().bind(model.isEditMode);
            termsTextArea.managedProperty().bind(model.isEditMode);
            termsTextArea.visibleProperty().bind(model.isEditMode);

            editButton.managedProperty().bind(model.isEditMode.not());
            editButton.visibleProperty().bind(model.isEditMode.not());
            buttonBar.managedProperty().bind(model.isEditMode);
            buttonBar.visibleProperty().bind(model.isEditMode);
            progressIndicator.managedProperty().bind(model.isPublishing);
            progressIndicator.visibleProperty().bind(model.isPublishing);

            cancelEditButton.disableProperty().bind(model.isPublishing);
            saveButton.disableProperty().bind(model.isPublishing);
            progressIndicator.progressProperty().bind(model.progress);

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    roboIconImageView.setImage(roboIcon);
                }
            });

            editButton.setOnAction(e -> controller.onEdit());
            cancelEditButton.setOnAction(e -> controller.onCancelEdit());
            saveButton.setOnAction(e -> controller.onSave(termsTextArea.getText(), bioTextArea.getText()));
        }

        @Override
        protected void onViewDetached() {
            nym.textProperty().unbind();
            nickName.textProperty().unbind();
            bio.textProperty().unbind();
            terms.textProperty().unbind();
            bioTextArea.textProperty().unbindBidirectional(model.bio);
            termsTextArea.textProperty().unbindBidirectional(model.terms);
            reputationScore.textProperty().unbind();
            profileAge.textProperty().unbind();

            bio.managedProperty().unbind();
            bio.visibleProperty().unbind();
            terms.managedProperty().unbind();
            terms.visibleProperty().unbind();

            bioTextArea.managedProperty().unbind();
            bioTextArea.visibleProperty().unbind();
            termsTextArea.managedProperty().unbind();
            termsTextArea.visibleProperty().unbind();

            editButton.managedProperty().unbind();
            editButton.visibleProperty().unbind();
            buttonBar.managedProperty().unbind();
            buttonBar.visibleProperty().unbind();
            progressIndicator.managedProperty().unbind();
            progressIndicator.visibleProperty().unbind();

            cancelEditButton.disableProperty().unbind();
            saveButton.disableProperty().unbind();
            progressIndicator.progressProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            editButton.setOnAction(null);
            cancelEditButton.setOnAction(null);
            saveButton.setOnAction(null);
        }

        private Pair<VBox, Label> getInfoBox(String title) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-4", "bisq-text-grey-9", "font-semi-bold");

            Label contentLabel = new Label();
            contentLabel.getStyleClass().addAll("bisq-text-6", "wrap-text");

            VBox box = new VBox(2, titleLabel, contentLabel);
            VBox.setMargin(box, new Insets(2, 0, 0, 0));

            return new Pair<>(box, contentLabel);
        }

        private Triple<VBox, Label, BisqTextArea> getEditableInfoBox(String title) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-4", "bisq-text-grey-9", "font-semi-bold");

            Label contentLabel = new Label();
            contentLabel.getStyleClass().addAll("bisq-text-6", "wrap-text");

            BisqTextArea editTextArea = new BisqTextArea();
            editTextArea.setManaged(false);
            editTextArea.setVisible(false);

            VBox box = new VBox(2, titleLabel, contentLabel, editTextArea);
            VBox.setMargin(box, new Insets(2, 0, 0, 0));

            return new Triple<>(box, contentLabel, editTextArea);
        }
    }
}