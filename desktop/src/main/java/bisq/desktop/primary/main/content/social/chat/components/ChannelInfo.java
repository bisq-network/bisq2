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

package bisq.desktop.primary.main.content.social.chat.components;

import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.ChatService;
import bisq.social.chat.PublicChannel;
import bisq.social.user.ChatUser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
        controller.model.setChannel(channel);
    }

    public void setOnUndoIgnoreChatUser(Runnable handler) {
        controller.model.undoIgnoreChatUserHandler = Optional.ofNullable(handler);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(ChatService chatService) {
            model = new Model(chatService);
            view = new View(model, this);
        }

        @Override
        public void onViewAttached() {
        }

        @Override
        public void onViewDetached() {
        }

        public void onUndoIgnoreUser(ChatUser chatUser) {
            model.chatService.undoIgnoreChatUser(chatUser);
            model.undoIgnoreChatUserHandler.ifPresent(Runnable::run);
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private ObjectProperty<Channel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private String channelName;
        private Optional<String> description = Optional.empty();
        private final ObservableList<ChatUserOverview> moderators = FXCollections.observableArrayList();
        private Optional<ChatUserOverview> adminProfile = Optional.empty();
        private final ObservableList<ChatUserOverview> members = FXCollections.observableArrayList();
        private Optional<Runnable> undoIgnoreChatUserHandler = Optional.empty();

        private Model(ChatService chatService) {
            this.chatService = chatService;
        }

        private void setChannel(Channel<? extends ChatMessage> channel) {
            channelName = channel.getChannelName();
            Set<String> ignoredChatUserIds = new HashSet<>(chatService.getPersistableStore().getIgnoredChatUserIds());
            members.setAll(channel.getChatMessages().stream()
                    .map(ChatMessage::getChatUser)
                    .distinct()
                    .map((chatUser -> new ChatUserOverview(chatUser, ignoredChatUserIds.contains(chatUser.id()))))
                    .sorted()
                    .collect(Collectors.toList()));
            if (channel instanceof PublicChannel publicChannel) {
                description = Optional.of(publicChannel.getDescription());
                adminProfile = Optional.of(new ChatUserOverview(publicChannel.getChannelAdmin()));
                moderators.setAll(publicChannel.getChannelModerators().stream()
                        .map(ChatUserOverview::new)
                        .sorted()
                        .collect(Collectors.toList()));

            } else {
                description = Optional.empty();
                adminProfile = Optional.empty();
                moderators.clear();
            }

            this.channel.set(channel);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private final ChangeListener<Channel> channelChangeListener;
        private ListView<ChatUserOverview> members;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(5);

            channelChangeListener = (observable, oldValue, newValue) -> update();
            update();
        }

        private void update() {
            if (model.channel.get() == null) {
                return;
            }

            root.getChildren().clear();

            BisqLabel channelName = new BisqLabel(model.channelName);
            channelName.getStyleClass().add("channel-settings-headline");
            root.getChildren().add(channelName);

            model.description.ifPresent(description -> root.getChildren().add(new BisqLabel(description)));

            model.adminProfile.ifPresent(adminProfile -> {
                BisqLabel adminHeadLine = new BisqLabel(Res.get("social.channel.settings.admin"));
                adminHeadLine.setPadding(new Insets(20, 0, -10, 0));
                adminHeadLine.getStyleClass().add("accent-headline");
                root.getChildren().addAll(adminHeadLine, adminProfile.getRoot());
            });

            if (!model.moderators.isEmpty()) {
                BisqLabel moderatorsHeadLine = new BisqLabel(Res.get("social.channel.settings.moderators"));
                moderatorsHeadLine.setPadding(new Insets(20, 0, -10, 0));
                moderatorsHeadLine.getStyleClass().add("accent-headline");
                root.getChildren().add(moderatorsHeadLine);
                root.getChildren().addAll(model.moderators.stream()
                        .map(ChatUserOverview::getRoot)
                        .collect(Collectors.toList()));
            }

            members = new ListView<>();
            members.setFocusTraversable(false);
            VBox.setVgrow(members, Priority.ALWAYS);
            members.setCellFactory(new Callback<>() {

                @Override
                public ListCell<ChatUserOverview> call(ListView<ChatUserOverview> list) {
                    return new ListCell<>() {
                        private BisqButton undoIgnoreUserButton;

                        @Override
                        public void updateItem(final ChatUserOverview chatUserOverview, boolean empty) {
                            super.updateItem(chatUserOverview, empty);
                            if (chatUserOverview != null && !empty) {
                                Pane chatUserOverviewRoot = chatUserOverview.getRoot();
                                undoIgnoreUserButton = new BisqButton(Res.get("social.undoIgnore"));
                                undoIgnoreUserButton.setOnAction(e -> {
                                    controller.onUndoIgnoreUser(chatUserOverview.getChatUser());
                                    members.refresh();
                                });

                                updateState(chatUserOverview, chatUserOverviewRoot);
                                HBox hBox = Layout.hBoxWith(chatUserOverviewRoot, Spacer.fillHBox(), undoIgnoreUserButton);
                                setGraphic(hBox);
                            } else {
                                if (undoIgnoreUserButton != null) {
                                    undoIgnoreUserButton.setOnAction(null);
                                }
                                setGraphic(null);
                            }
                        }

                        private void updateState(ChatUserOverview chatUserOverview, Pane chatUserOverviewRoot) {
                            undoIgnoreUserButton.setVisible(chatUserOverview.isIgnored());
                            chatUserOverviewRoot.setOpacity(chatUserOverview.isIgnored() ? 0.3 : 1);
                        }
                    };
                }
            });
            members.setMinWidth(400);
            members.setItems(model.members);
            BisqLabel membersHeadLine = new BisqLabel(Res.get("social.channel.settings.members"));
            membersHeadLine.setPadding(new Insets(20, 0, 0, 0));
            membersHeadLine.getStyleClass().add("accent-headline");
            root.getChildren().addAll(membersHeadLine, members);
        }

        @Override
        public void onViewAttached() {
            model.channel.addListener(channelChangeListener);
        }

        @Override
        protected void onViewDetached() {
            model.channel.removeListener(channelChangeListener);
        }
    }
}