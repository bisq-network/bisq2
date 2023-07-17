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

package bisq.desktop.main.content.chat.sidebar;

import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.i18n.Res;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ChannelSidebar {
    private final Controller controller;

    public ChannelSidebar(ServiceProvider serviceProvider,
                          Runnable closeHandler,
                          Consumer<UserProfile> openUserProfileSidebarHandler) {
        controller = new Controller(serviceProvider,
                closeHandler,
                openUserProfileSidebarHandler);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setChannel(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
        controller.setChannel(chatChannel);
    }

    public void setOnUndoIgnoreChatUser(Runnable handler) {
        controller.model.undoIgnoreChatUserHandler = Optional.ofNullable(handler);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final Runnable closeHandler;
        private final UserProfileService userProfileService;
        private final Consumer<UserProfile> openUserProfileSidebarHandler;
        private final NotificationsSidebar notificationsSidebar;
        private final ChatService chatService;
        private final BannedUserService bannedUserService;

        @Nullable
        private Pin userProfileIdsOfParticipantsPin;

        private Controller(ServiceProvider serviceProvider,
                           Runnable closeHandler,
                           Consumer<UserProfile> openUserProfileSidebarHandler) {
            this.closeHandler = closeHandler;
            this.openUserProfileSidebarHandler = openUserProfileSidebarHandler;

            userProfileService = serviceProvider.getUserService().getUserProfileService();
            chatService = serviceProvider.getChatService();
            bannedUserService = serviceProvider.getUserService().getBannedUserService();
            notificationsSidebar = new NotificationsSidebar(chatService);

            model = new Model();
            view = new View(model, this, notificationsSidebar.getRoot());
        }

        @Override
        public void onActivate() {
            model.getSortedListParticipantList()
                    .setComparator(Comparator.comparing(item -> item.getUserProfile().getUserName()));
        }

        @Override
        public void onDeactivate() {
            if (userProfileIdsOfParticipantsPin != null) {
                userProfileIdsOfParticipantsPin.unbind();
            }
        }

        void setChannel(@Nullable ChatChannel<? extends ChatMessage> chatChannel) {
            notificationsSidebar.setChannel(chatChannel);

            if (chatChannel == null) {
                model.descriptionVisible.set(false);
                model.description.set(null);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
                model.channel.set(null);
                return;
            }

            Set<String> ignoredChatUserIds = new HashSet<>(userProfileService.getIgnoredUserProfileIds());

            model.channelTitle.set(chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse(""));

            model.participantList.clear();
            if (userProfileIdsOfParticipantsPin != null) {
                userProfileIdsOfParticipantsPin.unbind();
            }
            userProfileIdsOfParticipantsPin = chatChannel.getUserProfileIdsOfParticipants().addListener(new CollectionObserver<>() {
                @Override
                public void add(String userProfileId) {
                    boolean ignored = ignoredChatUserIds.contains(userProfileId);
                    UIThread.run(() ->
                            userProfileService.findUserProfile(userProfileId)
                                    .ifPresent(userProfile -> model.participantList.add(new ChannelSidebarUserProfile(bannedUserService, userProfile, ignored))));
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof String) {
                        String userProfileId = (String) element;
                        UIThread.run(() ->
                                model.participantList.stream()
                                        .filter(item -> item.getUserProfile().getId().equals(userProfileId))
                                        .findFirst()
                                        .ifPresent(model.participantList::remove));
                    }
                }

                @Override
                public void clear() {
                    UIThread.run(model.participantList::clear);
                }
            });

            if (chatChannel instanceof CommonPublicChatChannel) {
                CommonPublicChatChannel commonPublicChatChannel = (CommonPublicChatChannel) chatChannel;
                model.description.set(commonPublicChatChannel.getDescription());
                model.descriptionVisible.set(true);
                model.adminProfile = commonPublicChatChannel.getChannelAdminId()
                        .flatMap(channelAdmin -> userProfileService.findUserProfile(channelAdmin).map(userProfile -> new ChannelSidebarUserProfile(bannedUserService, userProfile)))
                        .stream()
                        .findAny();
                model.moderators.setAll(commonPublicChatChannel.getChannelModeratorIds().stream()
                        .flatMap(id -> userProfileService.findUserProfile(id).stream())
                        .map(userProfile -> new ChannelSidebarUserProfile(bannedUserService, userProfile))
                        .sorted()
                        .collect(Collectors.toList()));
            } else if (chatChannel instanceof BisqEasyPublicChatChannel) {
                model.description.set(((BisqEasyPublicChatChannel) chatChannel).getDescription());
                model.descriptionVisible.set(true);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
            } else {
                model.descriptionVisible.set(false);
                model.description.set(null);
                model.adminProfile = Optional.empty();
                model.moderators.clear();
            }

            model.channel.set(chatChannel);
        }

        void onUndoIgnoreUser(UserProfile userProfile) {
            userProfileService.undoIgnoreUserProfile(userProfile);
            model.undoIgnoreChatUserHandler.ifPresent(Runnable::run);
        }

        void onClose() {
            closeHandler.run();
        }

        void onOpenUserProfileSidebar(UserProfile userProfile) {
            openUserProfileSidebarHandler.accept(userProfile);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ChatChannel<? extends ChatMessage>> channel = new SimpleObjectProperty<>();
        private final StringProperty channelTitle = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private final BooleanProperty descriptionVisible = new SimpleBooleanProperty();
        private final ObservableList<ChannelSidebarUserProfile> moderators = FXCollections.observableArrayList();
        private Optional<ChannelSidebarUserProfile> adminProfile = Optional.empty();
        private final ObservableList<ChannelSidebarUserProfile> participantList = FXCollections.observableArrayList();
        private final SortedList<ChannelSidebarUserProfile> sortedListParticipantList = new SortedList<>(participantList);
        private Optional<Runnable> undoIgnoreChatUserHandler = Optional.empty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ListView<ChannelSidebarUserProfile> participants;
        private final Label headline;
        private final MultiLineLabel descriptionText;
        private final Button closeButton;

        private View(Model model, Controller controller, Pane notificationsSidebar) {
            super(new VBox(), model, controller);

            root.setSpacing(15);
            root.setMinWidth(270);
            root.setPadding(new Insets(0, 20, 20, 20));

            closeButton = BisqIconButton.createIconButton("close");

            headline = new Label();
            headline.setId("chat-sidebar-headline");

            HBox.setMargin(headline, new Insets(18, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox topHBox = new HBox(headline, Spacer.fillHBox(), closeButton);

            descriptionText = new MultiLineLabel();
            descriptionText.setId("chat-sidebar-text");

            Label participantsLabel = new Label(Res.get("chat.sideBar.channelInfo.participants"));
            participantsLabel.setId("chat-sidebar-title");

            participants = new ListView<>(model.getSortedListParticipantList());
            VBox.setVgrow(participants, Priority.ALWAYS);
            participants.setCellFactory(getCellFactory(controller));

            VBox.setMargin(topHBox, new Insets(0, -20, 0, 0));
            VBox.setMargin(notificationsSidebar, new Insets(20, 0, 20, 0));
            root.getChildren().addAll(topHBox, descriptionText, notificationsSidebar, participantsLabel, participants);
        }

        @Override
        protected void onViewAttached() {
            headline.textProperty().bind(model.channelTitle);
            descriptionText.textProperty().bind(model.description);
            descriptionText.visibleProperty().bind(model.descriptionVisible);
            descriptionText.managedProperty().bind(model.descriptionVisible);
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            headline.textProperty().unbind();
            descriptionText.textProperty().unbind();
            descriptionText.visibleProperty().unbind();
            descriptionText.managedProperty().unbind();
            closeButton.setOnAction(null);
        }

        private Callback<ListView<ChannelSidebarUserProfile>, ListCell<ChannelSidebarUserProfile>> getCellFactory(Controller controller) {
            return new Callback<>() {
                @Override
                public ListCell<ChannelSidebarUserProfile> call(ListView<ChannelSidebarUserProfile> list) {
                    return new ListCell<>() {
                        Pane chatUser;
                        private ImageView roboIcon;
                        final Hyperlink undoIgnoreUserButton = new Hyperlink(Res.get("chat.sideBar.userProfile.undoIgnore"));
                        final HBox hBox = new HBox(10);

                        {
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.setFillHeight(true);
                            hBox.setPadding(new Insets(10, 10, 0, -10));
                            hBox.setCursor(Cursor.HAND);
                        }

                        @Override
                        public void updateItem(ChannelSidebarUserProfile channelSidebarUserProfile, boolean empty) {
                            super.updateItem(channelSidebarUserProfile, empty);
                            if (channelSidebarUserProfile != null && !empty) {
                                undoIgnoreUserButton.setOnAction(e -> {
                                    controller.onUndoIgnoreUser(channelSidebarUserProfile.getUserProfile());
                                    participants.refresh();
                                });
                                undoIgnoreUserButton.setVisible(channelSidebarUserProfile.isIgnored());
                                undoIgnoreUserButton.setManaged(channelSidebarUserProfile.isIgnored());

                                chatUser = channelSidebarUserProfile.getRoot();
                                chatUser.setOpacity(channelSidebarUserProfile.isIgnored() ? 0.4 : 1);
                                // With setOnMouseClicked or released it does not work well (prob. due handlers inside the components)
                                chatUser.setOnMousePressed(e -> controller.onOpenUserProfileSidebar(channelSidebarUserProfile.getUserProfile()));

                                roboIcon = channelSidebarUserProfile.getRoboIcon();
                                roboIcon.setOnMousePressed(e -> controller.onOpenUserProfileSidebar(channelSidebarUserProfile.getUserProfile()));

                                hBox.getChildren().setAll(chatUser, Spacer.fillHBox(), undoIgnoreUserButton);

                                setGraphic(hBox);
                            } else {
                                undoIgnoreUserButton.setOnAction(null);
                                if (chatUser != null) {
                                    chatUser.setOnMousePressed(null);
                                    chatUser = null;
                                }
                                if (roboIcon != null) {
                                    roboIcon.setOnMousePressed(null);
                                    roboIcon = null;
                                }
                                setGraphic(null);
                            }
                        }

                    };
                }
            };
        }
    }
}