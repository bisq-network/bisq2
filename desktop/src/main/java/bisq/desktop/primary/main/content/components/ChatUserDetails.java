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

package bisq.desktop.primary.main.content.components;

import bisq.common.data.ByteArray;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.common.utils.Layout;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUser;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ChatUserDetails implements Comparable<ChatUserDetails> {
    private final Controller controller;

    public ChatUserDetails(ChatService chatService, ChatUser chatUser) {
        controller = new Controller(chatService, chatUser);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setOnMentionUser(Consumer<ChatUser> handler) {
        controller.model.mentionUserHandler = Optional.ofNullable(handler);
    }

    public void setOnSendPrivateMessage(Consumer<ChatUser> handler) {
        controller.model.sendPrivateMessageHandler = Optional.ofNullable(handler);
    }

    public void setOnIgnoreChatUser(Runnable handler) {
        controller.model.ignoreChatUserHandler = Optional.ofNullable(handler);
    }

    @Override
    public int compareTo(ChatUserDetails o) {
        return controller.model.chatUser.getNym().compareTo(o.controller.model.chatUser.getNym());
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;


        private Controller(ChatService chatService, ChatUser chatUser) {
            model = new Model(chatService, chatUser);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            ChatUser chatUser = model.chatUser;
            if (chatUser == null) {
                return;
            }

            model.id.set(Res.get("social.createUserProfile.id", chatUser.getId()));
            model.bio.set(chatUser.getBio());
            model.burnScore.set(chatUser.getBurnScoreAsString());
            model.accountAge.set(chatUser.getAccountAgeAsString());

            model.nym.set(chatUser.getNym());
            model.nickName.set(chatUser.getNickName());
            model.roboHashNode.set(RoboHash.getImage(new ByteArray(chatUser.getPubKeyHash())));
            String entitledRoles = chatUser.getRoles().stream().map(e -> Res.get(e.type().name())).collect(Collectors.joining(", "));
            model.entitlements.set(Res.get("social.createUserProfile.entitledRoles", entitledRoles));
            model.entitlementsVisible.set(!chatUser.getRoles().isEmpty());
        }

        @Override
        public void onDeactivate() {
        }

        public void onSendPrivateMessage() {
            model.sendPrivateMessageHandler.ifPresent(handler -> handler.accept(model.chatUser));
        }

        public void onMentionUser() {
            model.mentionUserHandler.ifPresent(handler -> handler.accept(model.chatUser));
        }

        public void onIgnoreUser() {
            model.chatService.ignoreChatUser(model.chatUser);
            model.ignoreChatUserHandler.ifPresent(Runnable::run);
        }

        public void onReportUser() {
            // todo open popup for editing reason
            model.chatService.reportChatUser(model.chatUser, "");
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final ChatUser chatUser;
        private Optional<Consumer<ChatUser>> mentionUserHandler = Optional.empty();
        private Optional<Consumer<ChatUser>> sendPrivateMessageHandler = Optional.empty();
        private Optional<Runnable> ignoreChatUserHandler = Optional.empty();
        private final ObjectProperty<Image> roboHashNode = new SimpleObjectProperty<>();
        private final StringProperty nym = new SimpleStringProperty();
        private final StringProperty nickName = new SimpleStringProperty();
        private final StringProperty id = new SimpleStringProperty();
        private final StringProperty bio = new SimpleStringProperty();
        private final StringProperty burnScore = new SimpleStringProperty();
        private final StringProperty accountAge = new SimpleStringProperty();
        private final BooleanProperty entitlementsVisible = new SimpleBooleanProperty();
        private final StringProperty entitlements = new SimpleStringProperty();

        private Model(ChatService chatService, ChatUser chatUser) {
            this.chatService = chatService;
            this.chatUser = chatUser;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ImageView roboIconImageView;
        private final Label nym, nickName, id, entitlements, bio, burnScore, accountAge;
        private final Button openPrivateMessageButton, mentionButton, ignoreButton, reportButton;
        private Subscription roboHashNodeSubscription;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            root.setMaxWidth(200);
            root.setPadding(new Insets(0, 25, 0, 35));
            root.setAlignment(Pos.TOP_CENTER);

            nickName = new Label();
            nickName.getStyleClass().addAll("bisq-text-9", "font-semibold");
            nickName.setAlignment(Pos.CENTER);
            nickName.setMaxWidth(200);
            nickName.setMinWidth(200);
            VBox.setMargin(nickName, new Insets(-20, 0, 5, 0));

            roboIconImageView = new ImageView();

            nym = new Label();
            nym.getStyleClass().addAll("bisq-text-7");
            nym.setAlignment(Pos.CENTER);
            nym.setMaxWidth(200);
            nym.setMinWidth(200);
            VBox.setMargin(nym, new Insets(5, 0, 20, 0));


            VBox bioBox = getInfoBox(Res.get("social.chatUser.bio"), false);
            bio = (Label) bioBox.getChildren().get(1);

            VBox burnScoreBox = getInfoBox(Res.get("social.chatUser.burnScore"), false);
            burnScore = (Label) burnScoreBox.getChildren().get(1);

            VBox accountAgeBox = getInfoBox(Res.get("social.chatUser.accountAge"), false);
            accountAge = (Label) accountAgeBox.getChildren().get(1);

            VBox chatRulesBox = getInfoBox(Res.get("social.chat.chatRules.headline"), true);
            Label chatRules = (Label) chatRulesBox.getChildren().get(1);
            chatRules.setText(Res.get("social.chat.chatRules.content"));

            id = new Label();
            id.getStyleClass().add("offer-label-small"); //todo
            id.setPadding(new Insets(-5, 0, 0, 5));

            entitlements = new Label();
            entitlements.getStyleClass().add("offer-label-small"); //todo
            entitlements.setPadding(new Insets(-5, 0, 0, 0));

            //todo add reputation data. need more concept work

            openPrivateMessageButton = new Button(Res.get("social.sendPrivateMessage"));
            openPrivateMessageButton.getStyleClass().add("default-button");
            mentionButton = new Button(Res.get("social.mention"));
            mentionButton.getStyleClass().add("default-button");
            ignoreButton = new Button(Res.get("social.ignore"));
            ignoreButton.getStyleClass().add("default-button");
            reportButton = new Button(Res.get("social.report"));
            reportButton.getStyleClass().add("default-button");

            Region separator = Layout.separator();
            VBox.setMargin(separator, new Insets(30, -45, 30, -55));

            root.getChildren().addAll(nickName, roboIconImageView, nym, bioBox, burnScoreBox, accountAgeBox,
                    /* id, entitlements, */ Spacer.height(10), openPrivateMessageButton, mentionButton, ignoreButton,
                    reportButton, separator, chatRulesBox);
        }

        @Override
        protected void onViewAttached() {
            nym.textProperty().bind(model.nym);
            nickName.textProperty().bind(model.nickName);
            id.textProperty().bind(model.id);
            bio.textProperty().bind(model.bio);
            burnScore.textProperty().bind(model.burnScore);
            accountAge.textProperty().bind(model.accountAge);
            entitlements.textProperty().bind(model.entitlements);
            entitlements.visibleProperty().bind(model.entitlementsVisible);
            entitlements.managedProperty().bind(model.entitlementsVisible);
            mentionButton.minWidthProperty().bind(openPrivateMessageButton.widthProperty());
            ignoreButton.minWidthProperty().bind(openPrivateMessageButton.widthProperty());
            reportButton.minWidthProperty().bind(openPrivateMessageButton.widthProperty());

            roboHashNodeSubscription = EasyBind.subscribe(model.roboHashNode, roboIcon -> {
                if (roboIcon != null) {
                    // roboIconImageView.setImage(roboIcon);
                    roboIconImageView.setId("temp-robo-big-profile-icon");
                }
            });

            openPrivateMessageButton.setOnAction(e -> controller.onSendPrivateMessage());
            mentionButton.setOnAction(e -> controller.onMentionUser());
            ignoreButton.setOnAction(e -> controller.onIgnoreUser());
            reportButton.setOnAction(e -> controller.onReportUser());
        }

        @Override
        protected void onViewDetached() {
            nym.textProperty().unbind();
            nickName.textProperty().unbind();
            id.textProperty().unbind();
            bio.textProperty().unbind();
            burnScore.textProperty().unbind();
            accountAge.textProperty().unbind();
            entitlements.textProperty().unbind();
            entitlements.visibleProperty().unbind();
            entitlements.managedProperty().unbind();
            mentionButton.minWidthProperty().unbind();
            ignoreButton.minWidthProperty().unbind();
            reportButton.minWidthProperty().unbind();

            roboHashNodeSubscription.unsubscribe();

            openPrivateMessageButton.setOnAction(null);
            mentionButton.setOnAction(null);
            ignoreButton.setOnAction(null);
            reportButton.setOnAction(null);
        }

        private VBox getInfoBox(String title, boolean smaller) {
            Label titleLabel = new Label(title.toUpperCase());
            titleLabel.getStyleClass().addAll("bisq-text-7", "bisq-text-grey-9", "font-semibold");
            Label contentLabel = new Label();
            contentLabel.getStyleClass().addAll(smaller ? "bisq-text-7" : "bisq-text-6", "wrap-text");
            VBox box = new VBox(5, titleLabel, contentLabel);
            VBox.setMargin(box, new Insets(8, 0, 8, 0));
            return box;
        }
    }
}