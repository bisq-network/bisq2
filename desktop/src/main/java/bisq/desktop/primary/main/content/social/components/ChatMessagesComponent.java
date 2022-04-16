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

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTaggableTextArea;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilteredListItem;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.security.DigestUtil;
import bisq.social.chat.*;
import bisq.social.user.ChatUser;
import bisq.social.user.profile.UserProfile;
import bisq.social.user.profile.UserProfileService;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
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

import java.util.Date;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public void mentionUser(String userName) {
        controller.mentionUser(userName);
    }

  /*  public ObjectProperty<ChannelListItem<?>> selectedChannelListItemProperty() {
        return controller.model.selectedChannelListItem;
    }*/

    public void setSelectedChannelListItem(ChannelListItem<?> item) {
        controller.model.selectedChannelListItem.set(item);
    }

    public FilteredList<ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.model.getFilteredChatMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final UserProfileService userProfileService;
        private final QuotedMessageBlock quotedMessageBlock;
        private ListChangeListener<ChatMessagesComponent.ChatMessageListItem<? extends ChatMessage>> messageListener;
        private Pin chatMessagesPin, selectedChannelPin, tradeTagsPin, currencyTagsPin, paymentMethodTagsPin, customTagsPin;

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

            EasyBind.subscribe(model.selectedChannelListItem, item -> {
                if (item != null) {
                    log.error("getChatMessages {}",item.getChannel().getChatMessages());
                    model.chatMessages.setAll(item.getChannel().getChatMessages().stream()
                            .map(ChatMessageListItem::new)
                            .collect(Collectors.toSet()));
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
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        void onSendMessage(String text) {
            Channel<? extends ChatMessage> channel = model.selectedChannelListItem.get().getChannel();
            UserProfile userProfile = userProfileService.getSelectedUserProfile();
            if (channel instanceof MarketChannel marketChannel) {
                chatService.publishMarketChatTextMessage(text, quotedMessageBlock.getQuotedMessage(), marketChannel, userProfile);
            } else if (channel instanceof PublicChannel publicChannel) {
                chatService.publishPublicChatMessage(text, quotedMessageBlock.getQuotedMessage(), publicChannel, userProfile);
            } else if (channel instanceof PrivateChannel privateChannel) {
                chatService.sendPrivateChatMessage(text, quotedMessageBlock.getQuotedMessage(), privateChannel);
            }
            quotedMessageBlock.close();
        }

        public void onUserNameClicked(String userName) {
            mentionUser(userName);
        }

        public void onShowChatUserDetails(ChatMessage chatMessage) {
            //todo
           /* model.getSideBarVisible().set(true);
            model.getChannelInfoVisible().set(false);
            model.getNotificationsVisible().set(false);

            ChatUserDetails chatUserDetails = new ChatUserDetails(model.getChatService(), chatMessage.getAuthor());
            chatUserDetails.setOnSendPrivateMessage(chatUser -> {
                // todo
                log.info("onSendPrivateMessage {}", chatUser);
            });
            chatUserDetails.setOnIgnoreChatUser(this::refreshMessages);
            chatUserDetails.setOnMentionUser(chatUser -> chatMessagesComponent.mentionUser(chatUser.getProfileId()));
            model.setChatUserDetails(Optional.of(chatUserDetails));
            model.getChatUserDetailsRoot().set(chatUserDetails.getRoot());*/
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
                return; // should never happen as the button for opening the channel should not appear
                // but kept here for double safety
            }
            ChatUser peer = chatMessage.getAuthor();
            String channelId = PrivateChannel.createChannelId(peer, userProfileService.getPersistableStore().getSelectedUserProfile().get());
            PrivateChannel channel = chatService.getOrCreatePrivateChannel(channelId, peer);
            chatService.selectChannel(channel);
        }

        public void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            if (!chatService.isMyMessage(chatMessage)) {
                return;
            }
            if (chatMessage instanceof PublicChatMessage publicChatMessage) {
                UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
                chatService.publishEditedPublicChatMessage(publicChatMessage, editedText, userProfile)
                        .whenComplete((r, t) -> {
                            // todo maybe show spinner while deleting old msg and hide it once done?
                        });
            } else {
                //todo private message
            }
        }

        public void onDeleteMessage(ChatMessage chatMessage) {
            if (chatService.isMyMessage(chatMessage)) {
                if (chatMessage instanceof PublicChatMessage publicChatMessage) {
                    UserProfile userProfile = userProfileService.getPersistableStore().getSelectedUserProfile().get();
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

        public void onCreateOffer() {
            //todo
            //Navigation.navigateTo(NavigationTarget.ONBOARD_NEWBIE);
        }

        private void mentionUser(String userName) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + userName + " ");
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ChannelListItem<?>> selectedChannelListItem = new SimpleObjectProperty<>();

        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(chatMessages);
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(sortedChatMessages);

        private final StringProperty textInput = new SimpleStringProperty("");
        private final Predicate<ChatMessageListItem<? extends ChatMessage>> ignoredChatUserPredicate;
        private final ChatService chatService;
        private final UserProfileService userProfileService;

        private Model(ChatService chatService, UserProfileService userProfileService) {
            this.chatService = chatService;
            this.userProfileService = userProfileService;
            ignoredChatUserPredicate = item -> !chatService.getPersistableStore().getIgnoredChatUserIds().contains(item.getChatUserId());
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
        private final Button createOfferButton;
        private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;

        private View(Model model, Controller controller, Pane reply) {
            super(new VBox(), model, controller);

            root.setStyle("-fx-background-color: -fx-base");


            messagesListView = new ListView<>(model.getFilteredChatMessages());
            messagesListView.setCellFactory(getCellFactory());
            //messagesListView.setFocusTraversable(false);
            messagesListView.setStyle("-fx-border-width: 0; -fx-background-color: -fx-base");
            VBox.setVgrow(messagesListView, Priority.ALWAYS);

            inputField = new BisqTextArea();
            inputField.setLabelFloat(true);
            inputField.setPromptText(Res.get("social.chat.input.prompt"));
            inputField.setStyle("-fx-background-color: -fx-base");

            createOfferButton = new Button(Res.get("satoshisquareapp.chat.createOffer.button"));
            createOfferButton.setDefaultButton(true);
            HBox.setMargin(createOfferButton, new Insets(5, -30, 0, 0));

            HBox bottomBox = Layout.hBoxWith(inputField, createOfferButton);

            root.getChildren().addAll(messagesListView, reply, bottomBox);

            messagesListener = c -> messagesListView.scrollTo(model.getFilteredChatMessages().size() - 1);
        }

        @Override
        protected void onViewAttached() {
            inputField.textProperty().bindBidirectional(model.getTextInput());
            createOfferButton.setOnAction(e -> controller.onCreateOffer());

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
            // messagesListView.setItems(model.getFilteredChatMessages());
            model.getFilteredChatMessages().addListener(messagesListener);
        }

        @Override
        protected void onViewDetached() {
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            createOfferButton.setOnAction(null);
            inputField.setOnKeyPressed(null);
            model.getFilteredChatMessages().removeListener(messagesListener);
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
                        private final Label nickNameLabel = new Label();
                        private final Label time = new Label();
                        private final BisqTaggableTextArea message = new BisqTaggableTextArea();
                        private final Text quotedMessageField = new Text();
                        private final HBox hBox, reactionsBox, editControlsBox, quotedMessageBox;
                        private final VBox vBox, messageBox;
                        private final ChatUserIcon chatUserIcon = new ChatUserIcon(50);
                        Tooltip dateTooltip;
                        Subscription widthSubscription;

                        {
                            nickNameLabel.setId("chat-user-name");
                            nickNameLabel.setPadding(new Insets(2, 0, -8, 0));
                            time.getStyleClass().add("message-header");
                            time.setPadding(new Insets(-6, 0, 0, 0));
                            time.setVisible(false);

                            message.setAutoHeight(true);
                            VBox.setMargin(message, new Insets(0, 0, 0, 5));

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
                            reactionsBox.setStyle("-fx-background-color: -bisq-grey-left-nav-selected-bg; -fx-background-radius: 3px");
                            // reactionsBox.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");

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
                            VBox.setMargin(quotedMessageBox, new Insets(10, 0, 5, 0));
                            messageBox = Layout.vBoxWith(quotedMessageBox,
                                    message,
                                    editedMessageField,
                                    editControlsBox,
                                    Layout.hBoxWith(Spacer.fillHBox(), reactionsBox));
                            VBox.setVgrow(messageBox, Priority.ALWAYS);
                            vBox = Layout.vBoxWith(nickNameLabel, messageBox);
                            HBox.setHgrow(vBox, Priority.ALWAYS);
                            hBox = Layout.hBoxWith(Layout.vBoxWith(chatUserIcon, time), vBox);
                            setStyle("-fx-background-color: -fx-base");
                        }

                        @Override
                        public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                if (item.getQuotedMessage().isPresent()) {
                                    QuotedMessage quotedMessage = item.getQuotedMessage().get();
                                    if (quotedMessage.userName() != null &&
                                            quotedMessage.pubKeyHash() != null &&
                                            quotedMessage.message() != null) {
                                        Region verticalLine = new Region();
                                        verticalLine.setStyle("-fx-background-color: -bisq-grey-9");
                                        verticalLine.setMinWidth(3);
                                        verticalLine.setMinHeight(25);
                                        HBox.setMargin(verticalLine, new Insets(0, 0, 0, 5));

                                        quotedMessageField.setText(quotedMessage.message());
                                        quotedMessageField.setStyle("-fx-fill: -bisq-grey-9");

                                        Label userName = new Label(quotedMessage.userName());
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
                                }

                                message.setText(item.getMessage());
                              /*  message.setStyleSpans(0, KeyWordDetection.getStyleSpans(item.getMessage(),
                                        model.getTradeTags(),
                                        model.getCurrencyTags(),
                                        model.getPaymentMethodsTags(),
                                        model.getCustomTags()));*/

                                time.setText(item.getTime());

                                saveEditButton.setOnAction(e -> {
                                    controller.onSaveEditedMessage(item.getChatMessage(), editedMessageField.getText());
                                    onCloseEditMessage();
                                });
                                cancelEditButton.setOnAction(e -> onCloseEditMessage());

                                dateTooltip = new Tooltip(item.getDate());
                                Tooltip.install(time, dateTooltip);

                                nickNameLabel.setText(item.getAuthorUserName());
                                nickNameLabel.setOnMouseClicked(e -> controller.onUserNameClicked(item.getAuthorUserName()));

                                chatUserIcon.setChatUser(item.getAuthor(), model.getUserProfileService());
                                chatUserIcon.setCursor(Cursor.HAND);
                                chatUserIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
                                setOnMouseEntered(e -> {
                                    time.setVisible(true);
                                    reactionsBox.setVisible(true);
                                    messageBox.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");
                                    setStyle("-fx-background-color: -bisq-grey-left-nav-bg;");
                                });
                                setOnMouseExited(e -> {
                                    time.setVisible(false);
                                    reactionsBox.setVisible(false);
                                    messageBox.setStyle("-fx-background-color: -fx-base");
                                    setStyle("-fx-background-color: -fx-base");
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
                                editButton.setVisible(isMyMessage);
                                editButton.setManaged(isMyMessage);
                                deleteButton.setVisible(isMyMessage);
                                deleteButton.setManaged(isMyMessage);

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
                                nickNameLabel.setOnMouseClicked(null);
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
        private final String authorUserName;
        private final String time;
        private final String date;
        private final String chatUserId;
        private final ByteArray pubKeyHashAsByteArray;
        private final Optional<QuotedMessage> quotedMessage;
        private final ChatUser author;

        public ChatMessageListItem(T chatMessage) {
            this.chatMessage = chatMessage;
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            quotedMessage = chatMessage.getQuotedMessage();
            author = chatMessage.getAuthor();
            authorUserName = author.getProfileId();
            time = TimeFormatter.formatTime(new Date(chatMessage.getDate()));
            date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()));
            byte[] pubKeyHash = DigestUtil.hash(author.getNetworkId().getPubKey().publicKey().getEncoded());
            pubKeyHashAsByteArray = new ByteArray(pubKeyHash);
            chatUserId = Hex.encode(pubKeyHash);
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
                    StringUtils.containsIgnoreCase(authorUserName, filterString) ||
                    StringUtils.containsIgnoreCase(date, filterString);
        }
    }
}