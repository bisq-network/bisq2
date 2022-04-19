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

package bisq.desktop.primary.main.content.social.components;

import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyWordDetection;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTaggableTextArea;
import bisq.desktop.components.controls.jfx.BisqTextArea;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilteredListItem;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.social.chat.*;
import bisq.social.user.ChatUser;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static bisq.desktop.primary.main.content.social.components.ChatMessagesComponent.View.EDITED_POST_FIX;

@Slf4j
public class ChatMessagesComponent {
    private final Controller controller;

    public ChatMessagesComponent(ChatService chatService, UserProfileService userProfileService) {
        controller = new Controller(chatService, userProfileService);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void mentionUser(ChatUser chatUser) {
        controller.mentionUser(chatUser);
    }

    public FilteredList<ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.model.getFilteredChatMessages();
    }

    public void setOnShowChatUserDetails(Consumer<ChatUser> handler) {
        controller.model.showChatUserDetailsHandler = Optional.of(handler);
    }

    public void openPrivateChannel(ChatUser peer) {
        controller.openPrivateChannel(peer);
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }


    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final UserProfileService userProfileService;
        private final QuotedMessageBlock quotedMessageBlock;
        private ListChangeListener<ChatMessagesComponent.ChatMessageListItem<? extends ChatMessage>> messageListener;
        private Pin selectedChannelPin, chatMessagesPin;

        private Controller(ChatService chatService, UserProfileService userProfileService) {
            this.chatService = chatService;
            this.userProfileService = userProfileService;
            quotedMessageBlock = new QuotedMessageBlock();

            model = new Model(chatService, userProfileService);
            view = new View(model, this, quotedMessageBlock.getRoot());
        }

        @Override
        public void onActivate() {
            model.getSortedChatMessages().setComparator(ChatMessagesComponent.ChatMessageListItem::compareTo);
            model.customTags.addAll(chatService.getCustomTags());
            selectedChannelPin = chatService.getSelectedChannel().addObserver(channel -> {
                model.selectedChannel.set(channel);
                if (channel instanceof MarketChannel marketChannel) {
                    chatMessagesPin = FxBindings.<MarketChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                            .map(ChatMessageListItem::new)
                            .to(marketChannel.getChatMessages());
                    model.allowEditing.set(true);
                } else if (channel instanceof PublicChannel publicChannel) {
                    chatMessagesPin = FxBindings.<PublicChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                            .map(ChatMessageListItem::new)
                            .to(publicChannel.getChatMessages());
                    model.allowEditing.set(true);
                } else if (channel instanceof PrivateChannel privateChannel) {
                    chatMessagesPin = FxBindings.<PrivateChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                            .map(ChatMessageListItem::new)
                            .to(privateChannel.getChatMessages());
                    model.allowEditing.set(false);
                }
            });
            
       

          /*  selectedChannelPin = chatService.getPersistableStore().getSelectedChannel().addObserver(channel -> {
                if (channel instanceof PublicChannel publicChannel) {
                    if (messageListener != null) {
                        model.getChatMessages().removeListener(messageListener);
                    }

                    messageListener = c -> {
                        c.next();
                        // At init, we get full list, but we don't want to show notifications in that event.
                        if (c.getAddedSubList().equals(model.getChatMessages())) {
                            return;
                        }
                        if (channel.getNotificationSetting().get() == NotificationSetting.ALL) {
                            String messages = c.getAddedSubList().stream()
                                    .map(item -> item.getAuthorUserName() + ": " + item.getChatMessage().getText())
                                    .distinct()
                                    .collect(Collectors.joining("\n"));
                            if (!messages.isEmpty()) {
                                new Notification().headLine(messages).autoClose().hideCloseButton().show();
                            }
                        } else if (channel.getNotificationSetting().get() == NotificationSetting.MENTION) {
                            // TODO
                            // - Match only if mentioned username matches exactly (e.g. split item.getMessage() 
                            // in space separated tokens and compare those)
                            // - show user icon of sender (requires extending Notification to support custom graphics)
                            // 


                            // Notifications implementation is very preliminary. Not sure if another concept like its used in Element 
                            // would be better. E.g. Show all past notifications in the sidebar. When a new notification arrives, dont
                            // show the popup but only highlight the notifications icon (we would need to add a notification 
                            // settings tab then in the notifications component).
                            // We look up all our usernames, not only the selected one
                            Set<String> myUserNames = userProfileService.getPersistableStore().getUserProfiles().stream()
                                    .map(userProfile -> userProfile.getChatUser().getProfileId())
                                    .collect(Collectors.toSet());

                            String messages = c.getAddedSubList().stream()
                                    .filter(item -> myUserNames.stream().anyMatch(myUserName -> item.getMessage().contains("@" + myUserName)))
                                    .filter(item -> !myUserNames.contains(item.getAuthorUserName()))
                                    .map(item -> Res.get("social.notification.getMentioned",
                                            item.getAuthorUserName(),
                                            item.getChatMessage().getText()))
                                    .distinct()
                                    .collect(Collectors.joining("\n"));
                            if (!messages.isEmpty()) {
                                new Notification().headLine(messages).autoClose().hideCloseButton().show();
                            }
                        }
                    };
                    model.getChatMessages().addListener(messageListener);
                }
            });*/
        }

        @Override
        public void onDeactivate() {
            if (messageListener != null) {
                model.getChatMessages().removeListener(messageListener);
            }
            selectedChannelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        void onSendMessage(String text) {
            Channel<? extends ChatMessage> channel = model.selectedChannel.get();
            UserProfile userProfile = userProfileService.getSelectedUserProfile().get();
            if (channel instanceof MarketChannel marketChannel) {
                chatService.publishMarketChatTextMessage(text, quotedMessageBlock.getQuotedMessage(), marketChannel, userProfile);
            } else if (channel instanceof PublicChannel publicChannel) {
                chatService.publishPublicChatMessage(text, quotedMessageBlock.getQuotedMessage(), publicChannel, userProfile);
            } else if (channel instanceof PrivateChannel privateChannel) {
                chatService.sendPrivateChatMessage(text, quotedMessageBlock.getQuotedMessage(), privateChannel);
            }
            quotedMessageBlock.close();
        }

        public void onMention(ChatUser chatUser) {
            mentionUser(chatUser);
        }

        public void onShowChatUserDetails(ChatMessage chatMessage) {
            model.showChatUserDetailsHandler.ifPresent(handler -> handler.accept(chatMessage.getAuthor()));
        }

        public void onOpenEmojiSelector(ChatMessage chatMessage) {

        }

        public void onReply(ChatMessage chatMessage) {
            if (!chatService.isMyMessage(chatMessage)) {
                quotedMessageBlock.reply(chatMessage);
            }
        }

        /**
         * open a private channel to specified user. Automatically select it so user is ready to type the message to send.
         *
         * @param chatMessage
         */
        public void onOpenPrivateChannel(ChatMessage chatMessage) {
            if (chatService.isMyMessage(chatMessage)) {
                return;
            }
            openPrivateChannel(chatMessage.getAuthor());
        }

        private void openPrivateChannel(ChatUser peer) {
            String channelId = PrivateChannel.createChannelId(peer, userProfileService.getSelectedUserProfile().get());
            PrivateChannel channel = chatService.getOrCreatePrivateChannel(channelId, peer);
            chatService.setSelectedChannel(channel);
        }

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        public void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            if (!chatService.isMyMessage(chatMessage)) {
                return;
            }
            if (chatMessage instanceof MarketChatMessage marketChatMessage) {
                UserProfile userProfile = userProfileService.getSelectedUserProfile().get();
                chatService.publishEditedMarketChatMessage(marketChatMessage, editedText, userProfile)
                        .whenComplete((r, t) -> {
                            // todo maybe show spinner while deleting old msg and hide it once done?
                        });
            } else if (chatMessage instanceof PublicChatMessage publicChatMessage) {
                UserProfile userProfile = userProfileService.getSelectedUserProfile().get();
                chatService.publishEditedPublicChatMessage(publicChatMessage, editedText, userProfile)
                        .whenComplete((r, t) -> {
                            // todo maybe show spinner while deleting old msg and hide it once done?
                        });
            } else {
                //todo editing private message not supported yet
            }
        }

        public void onDeleteMessage(ChatMessage chatMessage) {
            if (chatService.isMyMessage(chatMessage)) {
                UserProfile userProfile = userProfileService.getSelectedUserProfile().get();
                if (chatMessage instanceof MarketChatMessage marketChatMessage) {
                    chatService.deletePublicChatMessage(marketChatMessage, userProfile);
                } else if (chatMessage instanceof PublicChatMessage publicChatMessage) {
                    chatService.deletePublicChatMessage(publicChatMessage, userProfile);
                } else {
                    //todo delete private message
                }
            }
        }

        public void onOpenMoreOptions(ChatMessage chatMessage) {

        }

        public void onAddEmoji(String emojiId) {

        }

  

        private void mentionUser(ChatUser chatUser) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + chatUser.getUserName() + " ");
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final UserProfileService userProfileService;
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final StringProperty textInput = new SimpleStringProperty("");
        private final Predicate<ChatMessageListItem<? extends ChatMessage>> ignoredChatUserPredicate;
        private final ObservableList<String> customTags = FXCollections.observableArrayList();
        private final BooleanProperty allowEditing = new SimpleBooleanProperty();
        private Optional<Consumer<ChatUser>> showChatUserDetailsHandler = Optional.empty();

        private Model(ChatService chatService, UserProfileService userProfileService) {
            this.chatService = chatService;
            this.userProfileService = userProfileService;
            ignoredChatUserPredicate = item -> !chatService.getIgnoredChatUserIds().contains(item.getAuthor().getId());
            filteredChatMessages.setPredicate(ignoredChatUserPredicate);
        }

        void setSendMessageResult(String channelId, ConfidentialMessageService.Result result, BroadcastResult broadcastResult) {
            log.info("Send message result for channelId {}: {}",
                    channelId, result.getState() + "; " + broadcastResult.toString()); //todo
        }

        void setSendMessageError(String channelId, Throwable throwable) {
            log.error("Send message resulted in an error: channelId={}, error={}", channelId, throwable.toString());  //todo
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return chatService.isMyMessage(chatMessage);
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final ListView<ChatMessageListItem<? extends ChatMessage>> messagesListView;
        private final BisqTextArea inputField;

        private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;
        private final HBox bottomBox;

        private View(Model model, Controller controller, Pane quotedMessageBlock) {
            super(new VBox(), model, controller);

            root.setSpacing(20);

            messagesListView = new ListView<>(model.getSortedChatMessages());
            Label placeholder = new Label(Res.get("table.placeholder.noData"));
            messagesListView.setPlaceholder(placeholder);
            messagesListView.setCellFactory(getCellFactory());
            VBox.setVgrow(messagesListView, Priority.ALWAYS);

            inputField = new BisqTextArea();
            inputField.setId("chat-messages-text-area-input-field");
            inputField.setPromptText(Res.get("social.chat.input.prompt"));
            inputField.setPrefWidth(300);


            // there will get added some controls for emojis so leave the box even its only 1 child yet
            bottomBox = Layout.hBoxWith(inputField);
            HBox.setHgrow(inputField, Priority.ALWAYS);

            bottomBox.setAlignment(Pos.CENTER);
            VBox.setMargin(bottomBox, new Insets(0, 0, -10, 0));
            root.getChildren().addAll(messagesListView, quotedMessageBlock, bottomBox);

            messagesListener = c -> messagesListView.scrollTo(model.getSortedChatMessages().size() - 1);
        }

        @Override
        protected void onViewAttached() {
            inputField.textProperty().bindBidirectional(model.getTextInput());

            inputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    if (event.isShiftDown()) {
                        inputField.appendText(System.getProperty("line.separator"));
                    } else if (!inputField.getText().isEmpty()) {
                        controller.onSendMessage(StringUtils.trimTrailingLinebreak(inputField.getText()));
                        inputField.clear();
                    }
                }
            });
            model.getSortedChatMessages().addListener(messagesListener);
        }

        @Override
        protected void onViewDetached() {
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            inputField.setOnKeyPressed(null);
            model.getSortedChatMessages().removeListener(messagesListener);
        }

        private Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                    return new ListCell<>() {
                        private final Button saveEditButton, cancelEditButton;
                        private final BisqTextArea editedMessageField;
                        private final Button emojiButton1, emojiButton2, openEmojiSelectorButton, replyButton,
                                pmButton, editButton, deleteButton, moreOptionsButton;
                        private final Label userNameLabel = new Label();
                        private final Label time = new Label();
                        private final BisqTaggableTextArea message = new BisqTaggableTextArea();
                        private final Text quotedMessageField = new Text();
                        private final HBox hBox, reactionsBox, editControlsBox, quotedMessageBox;
                        private final VBox vBox, messageBox;
                        private final ChatUserIcon chatUserIcon = new ChatUserIcon(37.5);
                        Tooltip dateTooltip;
                        Subscription widthSubscription;

                        {
                            userNameLabel.setId("chat-user-name");
                            userNameLabel.setPadding(new Insets(10, 0, -5, 0));

                            time.getStyleClass().add("message-header");
                            time.setPadding(new Insets(-6, 0, 0, 0));
                            time.setVisible(false);


                            //todo emojiButton1, emojiButton2, emojiButton3 will be filled with emoji icons
                            emojiButton1 = BisqIconButton.createIconButton(AwesomeIcon.THUMBS_UP_ALT);
                            emojiButton1.setUserData(":+1:");
                            emojiButton2 = BisqIconButton.createIconButton(AwesomeIcon.THUMBS_DOWN_ALT);
                            emojiButton1.setUserData(":-1:");
                            openEmojiSelectorButton = BisqIconButton.createIconButton(AwesomeIcon.DOUBLE_ANGLE_UP);
                            replyButton = BisqIconButton.createIconButton(AwesomeIcon.REPLY);
                            pmButton = BisqIconButton.createIconButton(AwesomeIcon.COMMENT_ALT);
                            editButton = BisqIconButton.createIconButton(AwesomeIcon.EDIT);
                            deleteButton = BisqIconButton.createIconButton(AwesomeIcon.REMOVE_SIGN);
                            moreOptionsButton = BisqIconButton.createIconButton(AwesomeIcon.ELLIPSIS_HORIZONTAL);
                            Label verticalLine = new Label("|");
                            HBox.setMargin(verticalLine, new Insets(4, 0, 0, 0));
                            verticalLine.setId("chat-message-reactions-separator");

                            editedMessageField = new BisqTextArea();
                            editedMessageField.setVisible(false);
                            editedMessageField.setManaged(false);

                            saveEditButton = new Button(Res.get("shared.save"));
                            cancelEditButton = new Button(Res.get("shared.cancel"));
                            editControlsBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
                            editControlsBox.setVisible(false);
                            editControlsBox.setManaged(false);
                            quotedMessageBox = new HBox();
                            quotedMessageBox.setSpacing(10);
                            quotedMessageBox.setVisible(false);
                            quotedMessageBox.setManaged(false);
                            VBox.setMargin(quotedMessageBox, new Insets(0, 0, 10, 0));

                            message.setAutoHeight(true);
                            VBox.setMargin(message, new Insets(-5, 0, 0, 0));

                            reactionsBox = Layout.hBoxWith(
                                    Spacer.fillHBox(),
                                    emojiButton1,
                                    emojiButton2,
                                    verticalLine,
                                    openEmojiSelectorButton,
                                    replyButton,
                                    pmButton,
                                    editButton,
                                    deleteButton,
                                    moreOptionsButton);
                            reactionsBox.setSpacing(5);
                            reactionsBox.setVisible(false);
                            reactionsBox.setStyle("-fx-background-color: -bisq-grey-18; -fx-background-radius: 8px");

                            HBox reactionsOuterBox = Layout.hBoxWith(Spacer.fillHBox(), reactionsBox);
                            VBox.setMargin(reactionsOuterBox, new Insets(10, 0, 0, 0));

                            messageBox = Layout.vBoxWith(quotedMessageBox,
                                    message,
                                    editedMessageField,
                                    editControlsBox,
                                    reactionsOuterBox);
                            messageBox.setSpacing(0);
                            VBox.setVgrow(messageBox, Priority.ALWAYS);
                            vBox = Layout.vBoxWith(userNameLabel, messageBox);
                            HBox.setHgrow(vBox, Priority.ALWAYS);
                            VBox userIconTimeBox = Layout.vBoxWith(chatUserIcon, time);
                            HBox.setMargin(userIconTimeBox, new Insets(10, 0, 0, -10));
                            this.hBox = Layout.hBoxWith(userIconTimeBox, vBox);
                        }

                        @Override
                        public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                if (item.getQuotedMessage().isPresent()) {
                                    quotedMessageBox.setVisible(true);
                                    quotedMessageBox.setManaged(true);
                                    QuotedMessage quotedMessage = item.getQuotedMessage().get();
                                    if (quotedMessage.nickName() != null &&
                                            quotedMessage.profileId() != null &&
                                            quotedMessage.pubKeyHash() != null &&
                                            quotedMessage.message() != null) {
                                        Region verticalLine = new Region();
                                        verticalLine.setStyle("-fx-background-color: -bisq-grey-9");
                                        verticalLine.setMinWidth(3);
                                        verticalLine.setMinHeight(25);
                                        HBox.setMargin(verticalLine, new Insets(0, 0, 0, 5));

                                        quotedMessageField.setText(quotedMessage.message());
                                        quotedMessageField.setStyle("-fx-fill: -bisq-grey-9");

                                        Label userName = new Label(quotedMessage.getUserName());
                                        userName.setPadding(new Insets(4, 0, 0, 0));
                                        userName.setStyle("-fx-text-fill: -bisq-grey-9");

                                        ImageView roboIconImageView = new ImageView();
                                        roboIconImageView.setFitWidth(25);
                                        roboIconImageView.setFitHeight(25);
                                        Image image = RoboHash.getImage(quotedMessage.pubKeyHash());
                                        roboIconImageView.setImage(image);

                                        HBox.setMargin(roboIconImageView, new Insets(0, 0, 0, -5));
                                        HBox iconAndUserName = Layout.hBoxWith(roboIconImageView, userName);
                                        iconAndUserName.setSpacing(5);

                                        VBox contentBox = Layout.vBoxWith(iconAndUserName, quotedMessageField);
                                        contentBox.setSpacing(5);
                                        quotedMessageBox.getChildren().setAll(verticalLine, contentBox);
                                        UIThread.runOnNextRenderFrame(() -> verticalLine.setMinHeight(contentBox.getHeight() - 10));
                                    }
                                } else {
                                    quotedMessageBox.getChildren().clear();
                                    quotedMessageBox.setVisible(false);
                                    quotedMessageBox.setManaged(false);
                                }

                                message.setText(item.getMessage());
                                message.setStyleSpans(0, KeyWordDetection.getStyleSpans(item.getMessage(), model.getCustomTags()));

                                time.setText(item.getTime());

                                saveEditButton.setOnAction(e -> {
                                    controller.onSaveEditedMessage(item.getChatMessage(), editedMessageField.getText());
                                    onCloseEditMessage();
                                });
                                cancelEditButton.setOnAction(e -> onCloseEditMessage());

                                dateTooltip = new Tooltip(item.getDate());
                                Tooltip.install(time, dateTooltip);

                                userNameLabel.setText(item.getAuthor().getUserName());
                                userNameLabel.setOnMouseClicked(e -> controller.onMention(item.getAuthor()));

                                chatUserIcon.setChatUser(item.getAuthor(), model.getUserProfileService());
                                chatUserIcon.setCursor(Cursor.HAND);
                                chatUserIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
                                Tooltip.install(chatUserIcon, new Tooltip(item.getAuthor().getTooltipString()));

                                setOnMouseEntered(e -> {
                                    time.setVisible(true);
                                    reactionsBox.setVisible(true);
                                    messageBox.setStyle("-fx-background-color: -bisq-grey-2");
                                    setStyle("-fx-background-color: -bisq-grey-2;");
                                });
                                setOnMouseExited(e -> {
                                    time.setVisible(false);
                                    reactionsBox.setVisible(false);
                                    messageBox.setStyle("-fx-background-color: transparent");
                                    setStyle("-fx-background-color: transparent");
                                });

                                ChatMessage chatMessage = item.getChatMessage();
                                emojiButton1.setOnAction(e -> controller.onAddEmoji((String) emojiButton1.getUserData()));
                                emojiButton2.setOnAction(e -> controller.onAddEmoji((String) emojiButton2.getUserData()));
                                openEmojiSelectorButton.setOnAction(e -> controller.onOpenEmojiSelector(chatMessage));
                                replyButton.setOnAction(e -> controller.onReply(chatMessage));
                                pmButton.setOnAction(e -> controller.onOpenPrivateChannel(chatMessage));
                                editButton.setOnAction(e -> onEditMessage(item));
                                deleteButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                                moreOptionsButton.setOnAction(e -> controller.onOpenMoreOptions(chatMessage));

                                boolean isMyMessage = model.isMyMessage(chatMessage);
                                replyButton.setVisible(!isMyMessage);
                                replyButton.setManaged(!isMyMessage);
                                pmButton.setVisible(!isMyMessage);
                                pmButton.setManaged(!isMyMessage);
                                editButton.setVisible(isMyMessage && model.allowEditing.get());
                                editButton.setManaged(isMyMessage && model.allowEditing.get());
                                deleteButton.setVisible(isMyMessage && model.allowEditing.get());
                                deleteButton.setManaged(isMyMessage && model.allowEditing.get());

                                widthSubscription = EasyBind.subscribe(messagesListView.widthProperty(),
                                        width -> {
                                            double wrappingWidth = width.doubleValue() - 95;
                                            quotedMessageField.setWrappingWidth(wrappingWidth - 20);
                                        });

                                setGraphic(hBox);
                            } else {
                                if (widthSubscription != null) {
                                    widthSubscription.unsubscribe();
                                }
                                userNameLabel.setOnMouseClicked(null);
                                chatUserIcon.setOnMouseClicked(null);
                                hBox.setOnMouseEntered(null);
                                hBox.setOnMouseExited(null);
                                chatUserIcon.releaseResources();

                                emojiButton1.setOnAction(null);
                                emojiButton2.setOnAction(null);
                                openEmojiSelectorButton.setOnAction(null);
                                replyButton.setOnAction(null);
                                pmButton.setOnAction(null);
                                editButton.setOnAction(null);
                                deleteButton.setOnAction(null);
                                moreOptionsButton.setOnAction(null);
                                saveEditButton.setOnAction(null);
                                cancelEditButton.setOnAction(null);
                                editedMessageField.setOnKeyPressed(null);

                                setGraphic(null);
                            }
                        }

                        private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                            editedMessageField.setVisible(true);
                            editedMessageField.setManaged(true);
                            editedMessageField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                            editedMessageField.setInitialHeight(message.getHeight());
                            editedMessageField.setScrollHideThreshold(200);

                            editControlsBox.setVisible(true);
                            editControlsBox.setManaged(true);
                            message.setVisible(false);
                            message.setManaged(false);
                            editedMessageField.setOnKeyPressed(event -> {
                                if (event.getCode() == KeyCode.ENTER) {
                                    event.consume();
                                    if (event.isShiftDown()) {
                                        editedMessageField.appendText(System.getProperty("line.separator"));
                                    } else if (!editedMessageField.getText().isEmpty()) {
                                        controller.onSaveEditedMessage(item.getChatMessage(),
                                                StringUtils.trimTrailingLinebreak(editedMessageField.getText()));
                                        onCloseEditMessage();
                                    }
                                }
                            });
                        }

                        private void onCloseEditMessage() {
                            editedMessageField.setVisible(false);
                            editedMessageField.setManaged(false);
                            editControlsBox.setVisible(false);
                            editControlsBox.setManaged(false);
                            message.setVisible(true);
                            message.setManaged(true);
                            editedMessageField.setOnKeyPressed(null);
                        }
                    };
                }
            };
        }
    }

    @Slf4j
    @Getter
    @EqualsAndHashCode
    public static class ChatMessageListItem<T extends ChatMessage> implements Comparable<ChatMessageListItem<T>>, FilteredListItem {
        private final T chatMessage;
        private final String message;
        private final String time;
        private final String date;
        private final Optional<QuotedMessage> quotedMessage;
        private final ChatUser author;

        public ChatMessageListItem(T chatMessage) {
            this.chatMessage = chatMessage;
            author = chatMessage.getAuthor();
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            quotedMessage = chatMessage.getQuotedMessage();
            time = TimeFormatter.formatTime(new Date(chatMessage.getDate()));
            date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()));
        }

        @Override
        public int compareTo(ChatMessageListItem o) {
            return new Date(chatMessage.getDate()).compareTo(new Date(o.getChatMessage().getDate()));
        }

        @Override
        public boolean match(String filterString) {
            return filterString == null ||
                    filterString.isEmpty() ||
                    StringUtils.containsIgnoreCase(message, filterString) ||
                    StringUtils.containsIgnoreCase(author.getProfileId(), filterString) ||
                    StringUtils.containsIgnoreCase(author.getNickName(), filterString) ||
                    StringUtils.containsIgnoreCase(date, filterString);
        }
    }
}