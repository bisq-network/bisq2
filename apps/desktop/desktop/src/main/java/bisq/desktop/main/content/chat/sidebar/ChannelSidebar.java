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

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

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

            userProfileIdsOfParticipantsPin = FxBindings.<String, ListItem>bind(model.participantList)
                    .filter(profileId -> userProfileService.findUserProfile(profileId).isPresent())
                    .map(profileId -> new ListItem(userProfileService.findUserProfile(profileId).orElseThrow(),
                            bannedUserService,
                            ignoredChatUserIds))
                    .to(chatChannel.getUserProfileIdsOfActiveParticipants());

            if (chatChannel instanceof CommonPublicChatChannel commonPublicChatChannel) {
                model.description.set(commonPublicChatChannel.getDescription());
                model.descriptionVisible.set(true);
            } else if (chatChannel instanceof BisqEasyOfferbookChannel) {
                model.description.set(((BisqEasyOfferbookChannel) chatChannel).getDescription());
                model.descriptionVisible.set(true);
            } else {
                model.descriptionVisible.set(false);
                model.description.set(null);
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
        private final ObservableList<ListItem> participantList = FXCollections.observableArrayList();
        private final SortedList<ListItem> sortedListParticipantList = new SortedList<>(participantList);
        private Optional<Runnable> undoIgnoreChatUserHandler = Optional.empty();

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final ListView<ListItem> participants;
        private final Label headline;
        private final Button closeButton;
        private final Text description;

        private View(Model model, Controller controller, Pane notificationsSidebar) {
            super(new VBox(), model, controller);

            root.setSpacing(15);
            root.setMinWidth(260);
            root.setMaxWidth(260);
            root.setPadding(new Insets(0, 20, 20, 20));

            closeButton = BisqIconButton.createIconButton("close");

            headline = new Label();
            headline.setId("chat-sidebar-headline");

            HBox.setMargin(headline, new Insets(18, 0, 0, 0));
            HBox.setMargin(closeButton, new Insets(10, 10, 0, 0));
            HBox topHBox = new HBox(headline, Spacer.fillHBox(), closeButton);

            description = new Text();
            TextFlow descriptionTextFlow = new TextFlow(description);
            description.setId("chat-sidebar-text");

            Label participantsLabel = new Label(Res.get("chat.sideBar.channelInfo.participants"));
            participantsLabel.setId("chat-sidebar-title");

            participants = new ListView<>(model.getSortedListParticipantList());
            VBox.setVgrow(participants, Priority.ALWAYS);
            participants.setCellFactory(getCellFactory(controller));

            VBox.setMargin(topHBox, new Insets(0, -20, 0, 0));
            VBox.setMargin(notificationsSidebar, new Insets(20, 0, 20, 0));
            root.getChildren().addAll(topHBox, descriptionTextFlow, notificationsSidebar, participantsLabel, participants);
        }

        @Override
        protected void onViewAttached() {
            headline.textProperty().bind(model.channelTitle);
            description.textProperty().bind(model.description);
            description.visibleProperty().bind(model.descriptionVisible);
            description.managedProperty().bind(model.descriptionVisible);
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            headline.textProperty().unbind();
            description.textProperty().unbind();
            description.visibleProperty().unbind();
            description.managedProperty().unbind();
            closeButton.setOnAction(null);
        }

        private Callback<ListView<ListItem>, ListCell<ListItem>> getCellFactory(
                Controller controller) {
            return new Callback<>() {
                @Override
                public ListCell<ListItem> call(ListView<ListItem> list) {
                    return new ListCell<>() {
                        private final Label userName = new Label();
                        private final BisqTooltip tooltip = new BisqTooltip();
                        private final ImageView catHashImageView = new ImageView();
                        private final Hyperlink undoIgnoreUserButton = new Hyperlink(Res.get("chat.sideBar.userProfile.undoIgnore"));
                        private final HBox userHBox = new HBox(10, catHashImageView, userName);
                        private final HBox hBox = new HBox(10, userHBox, Spacer.fillHBox(), undoIgnoreUserButton);

                        {
                            userHBox.setAlignment(Pos.CENTER_LEFT);

                            userName.getStyleClass().add("text-fill-white");
                            userName.setMaxWidth(100);

                            catHashImageView.setFitWidth(37.5);
                            catHashImageView.setFitHeight(catHashImageView.getFitWidth());

                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.setFillHeight(true);
                            hBox.setPadding(new Insets(0, 10, 0, 0));
                            hBox.setCursor(Cursor.HAND);
                        }

                        @Override
                        protected void updateItem(ListItem item, boolean empty) {
                            super.updateItem(item, empty);

                            if (item != null && !empty) {
                                boolean isIgnored = item.isIgnored();
                                UserProfile userProfile = item.getUserProfile();

                                userName.setText(item.getUserName());
                                if (item.isBanned()) {
                                    userName.getStyleClass().add("error");
                                }

                                catHashImageView.setImage(CatHash.getImage(userProfile));

                                tooltip.setText(item.getTooltipString());
                                Tooltip.install(userHBox, tooltip);

                                undoIgnoreUserButton.setVisible(isIgnored);
                                undoIgnoreUserButton.setManaged(isIgnored);

                                userHBox.setOpacity(isIgnored ? 0.4 : 1);

                                // With setOnMouseClicked or released it does not work well (prob. due handlers inside the components)
                                userHBox.setOnMousePressed(e -> controller.onOpenUserProfileSidebar(userProfile));
                                // catHashImageView.setOnMousePressed(e -> controller.onOpenUserProfileSidebar(item.getUserProfile()));
                                undoIgnoreUserButton.setOnAction(e -> {
                                    controller.onUndoIgnoreUser(userProfile);
                                    participants.refresh();
                                });
                                setGraphic(hBox);
                            } else {
                                undoIgnoreUserButton.setOnAction(null);
                                userHBox.setOnMousePressed(null);
                                catHashImageView.setImage(null);
                                Tooltip.uninstall(hBox, tooltip);
                                setGraphic(null);
                            }
                        }
                    };
                }
            };
        }
    }

    @Slf4j
    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ListItem implements Comparable<ListItem> {
        @EqualsAndHashCode.Include
        private final UserProfile userProfile;

        private final BannedUserService bannedUserService;
        private final boolean ignored;
        private final boolean isBanned;
        private final String userName, tooltipString;

        private ListItem(UserProfile userProfile, BannedUserService bannedUserService, Set<String> ignoredChatUserIds) {
            this.userProfile = userProfile;

            this.bannedUserService = bannedUserService;
            this.ignored = ignoredChatUserIds.contains(userProfile.getId());
            isBanned = bannedUserService.isUserProfileBanned(userProfile);
            userName = isBanned ? Res.get("user.userProfile.userName.banned", userProfile.getUserName()) : userProfile.getUserName();
            String banPrefix = isBanned ? Res.get("user.userProfile.tooltip.banned") + "\n" : "";
            tooltipString = banPrefix + userProfile.getTooltipString();
        }

        @Override
        public int compareTo(ListItem o) {
            return o.userProfile.getUserName().compareTo(o.userProfile.getUserName());
        }
    }
}