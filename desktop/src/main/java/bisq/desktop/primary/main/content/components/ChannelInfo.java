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

import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.Channel;
import bisq.social.chat.channels.PublicDiscussionChannel;
import bisq.social.chat.channels.PublicTradeChannel;
import bisq.social.chat.messages.ChatMessage;
import bisq.user.PublicUserProfile;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ChannelInfo {
    private final Controller controller;

    public ChannelInfo(ChatService chatService) {
        controller = new Controller(chatService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setChannel(Channel<? extends ChatMessage> channel) {
        controller.setChannel(channel);
    }

    public void setOnUndoIgnoreChatUser(Runnable handler) {
        controller.model.undoIgnoreChatUserHandler = Optional.ofNullable(handler);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        private final ChatService chatService;
        @Getter
        private final View view;

        private Controller(ChatService chatService) {
            this.chatService = chatService;
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        public void onUndoIgnoreUser(PublicUserProfile publicUserProfile) {
            chatService.undoIgnoreChatUser(publicUserProfile);
            model.undoIgnoreChatUserHandler.ifPresent(Runnable::run);
        }

        public void setChannel(Channel<? extends ChatMessage> channel) {
            Set<String> ignoredChatUserIds = new HashSet<>(chatService.getIgnoredChatUserIds());
            model.channelName.set(channel.getDisplayString());
            model.members.setAll(channel.getChatMessages().stream()
                    .flatMap(chatMessage -> chatService.findChatUser(chatMessage.getAuthorId()).stream())
                    .distinct()
                    .map((chatUser -> new ChatUserOverview(chatUser, ignoredChatUserIds.contains(chatUser.getId()))))
                    .sorted()
                    .collect(Collectors.toList()));

            if (channel instanceof PublicTradeChannel) {
                model.description.set(((PublicTradeChannel) channel).getDescription());
                model.descriptionVisible.set(true);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
            } else if (channel instanceof PublicDiscussionChannel) {
                PublicDiscussionChannel publicDiscussionChannel = (PublicDiscussionChannel) channel;
                model.description.set(publicDiscussionChannel.getDescription());
                model.descriptionVisible.set(true);
                model.adminProfile = chatService.findChatUser(publicDiscussionChannel.getChannelAdminId()).map(ChatUserOverview::new);
                model.moderators.setAll(publicDiscussionChannel.getChannelModeratorIds().stream()
                        .flatMap(id -> chatService.findChatUser(id).stream())
                        .map(ChatUserOverview::new)
                        .sorted()
                        .collect(Collectors.toList()));
            } else {
                model.descriptionVisible.set(false);
                model.description.set(null);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
            }

            model.channel.set(channel);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Channel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final StringProperty channelName = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private final BooleanProperty descriptionVisible = new SimpleBooleanProperty();
        private final ObservableList<ChatUserOverview> moderators = FXCollections.observableArrayList();
        private Optional<ChatUserOverview> adminProfile = Optional.empty();
        private final ObservableList<ChatUserOverview> members = FXCollections.observableArrayList();
        private Optional<Runnable> undoIgnoreChatUserHandler = Optional.empty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final ListView<ChatUserOverview> members;
        private final Label channelNameLabel;
        private final Text descriptionText;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(15);

            channelNameLabel = new Label();
            channelNameLabel.setId("chat-sidebar-headline");

            descriptionText = new Text();
            descriptionText.setId("chat-sidebar-text");

            Label membersHeadLine = new Label(Res.get("social.channel.settings.members").toUpperCase());
            membersHeadLine.setId("chat-sidebar-text");
            VBox.setMargin(membersHeadLine, new Insets(20, 0, 0, 0));

            members = new ListView<>(model.members);
            VBox.setVgrow(members, Priority.ALWAYS);
            members.setCellFactory(new Callback<>() {
                @Override
                public ListCell<ChatUserOverview> call(ListView<ChatUserOverview> list) {
                    return new ListCell<>() {
                        final Button undoIgnoreUserButton = new Button(Res.get("social.undoIgnore"));
                        final HBox hBox = new HBox();

                        {
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.setSpacing(10);
                            hBox.setFillHeight(true);
                            setCursor(Cursor.HAND);
                            setPrefHeight(40);
                            setPadding(new Insets(0, 0, -20, 0));
                        }

                        @Override
                        public void updateItem(final ChatUserOverview chatUserOverview, boolean empty) {
                            super.updateItem(chatUserOverview, empty);
                            if (chatUserOverview != null && !empty) {
                                undoIgnoreUserButton.setOnAction(e -> {
                                    controller.onUndoIgnoreUser(chatUserOverview.getChatUser());
                                    members.refresh();
                                });
                                undoIgnoreUserButton.setVisible(chatUserOverview.isIgnored());
                                undoIgnoreUserButton.setManaged(chatUserOverview.isIgnored());

                                Pane chatUserOverviewRoot = chatUserOverview.getRoot();
                                chatUserOverviewRoot.setOpacity(chatUserOverview.isIgnored() ? 0.4 : 1);

                                hBox.getChildren().setAll(chatUserOverviewRoot, Spacer.fillHBox(), undoIgnoreUserButton);

                                setGraphic(hBox);
                            } else {
                                if (undoIgnoreUserButton != null) {
                                    undoIgnoreUserButton.setOnAction(null);
                                }
                                setGraphic(null);
                            }
                        }

                    };
                }
            });

            root.getChildren().addAll(channelNameLabel, descriptionText, membersHeadLine, members);

            //todo admin/mods not added yet
                 /*  model.adminProfile.ifPresent(adminProfile -> {
                    Label adminHeadLine = new Label(Res.get("social.channel.settings.admin"));
                    adminHeadLine.setPadding(new Insets(20, 0, 0, 0));
                    root.getChildren().addAll(adminHeadLine, adminProfile.getRoot());
                });

                if (!model.moderators.isEmpty()) {
                    Label moderatorsHeadLine = new Label(Res.get("social.channel.settings.moderators"));
                    moderatorsHeadLine.setPadding(new Insets(20, 0, 0, 0));
                    root.getChildren().add(moderatorsHeadLine);
                    root.getChildren().addAll(model.moderators.stream()
                            .map(ChatUserOverview::getRoot)
                            .collect(Collectors.toList()));
                }*/
        }

        @Override
        protected void onViewAttached() {
            channelNameLabel.textProperty().bind(model.channelName);
            descriptionText.textProperty().bind(model.description);
            descriptionText.visibleProperty().bind(model.descriptionVisible);
            descriptionText.managedProperty().bind(model.descriptionVisible);
            EasyBind.subscribe(root.widthProperty(), w -> {
                double width = w.doubleValue();
                if (width > 0) {
                    descriptionText.setWrappingWidth(width - 20);
                }
            });
        }

        @Override
        protected void onViewDetached() {
            channelNameLabel.textProperty().unbind();
            descriptionText.textProperty().unbind();
            descriptionText.visibleProperty().unbind();
            descriptionText.managedProperty().unbind();
        }
    }
}