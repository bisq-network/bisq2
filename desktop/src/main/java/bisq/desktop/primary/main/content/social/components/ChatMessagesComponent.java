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
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.KeyWordDetection;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopupMenu;
import bisq.desktop.components.controls.BisqPopupMenuItem;
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
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.*;
import bisq.social.chat.messages.*;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import bisq.social.user.reputation.ReputationScore;
import bisq.social.user.reputation.ReputationService;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.binding.Bindings;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static bisq.desktop.primary.main.content.social.components.ChatMessagesComponent.View.EDITED_POST_FIX;

@Slf4j
public class ChatMessagesComponent {
    private final Controller controller;

    public ChatMessagesComponent(ChatService chatService,
                                 ChatUserService chatUserService,
                                 ReputationService reputationService,
                                 boolean isDiscussionsChat) {
        controller = new Controller(chatService, chatUserService, reputationService, isDiscussionsChat);
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
        controller.createAndSelectPrivateChannel(peer);
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }


    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final ChatUserService chatUserService;
        private final ReputationService reputationService;
        private final QuotedMessageBlock quotedMessageBlock;
        private ListChangeListener<ChatMessagesComponent.ChatMessageListItem<? extends ChatMessage>> messageListener;
        private Pin selectedChannelPin, chatMessagesPin;

        private Controller(ChatService chatService,
                           ChatUserService chatUserService,
                           ReputationService reputationService,
                           boolean isDiscussionsChat) {
            this.chatService = chatService;
            this.chatUserService = chatUserService;
            this.reputationService = reputationService;
            quotedMessageBlock = new QuotedMessageBlock(chatService);

            model = new Model(chatService, chatUserService, reputationService, isDiscussionsChat);
            view = new View(model, this, quotedMessageBlock.getRoot());
        }

        @Override
        public void onActivate() {
            model.getSortedChatMessages().setComparator(ChatMessagesComponent.ChatMessageListItem::compareTo);
            model.customTags.addAll(chatService.getCustomTags());

            if (model.isDiscussionsChat) {
                selectedChannelPin = chatService.getSelectedDiscussionChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicDiscussionChannel publicDiscussionChannel) {
                        chatMessagesPin = FxBindings.<PublicDiscussionChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, chatService))
                                .to(publicDiscussionChannel.getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateDiscussionChannel privateDiscussionChannel) {
                        chatMessagesPin = FxBindings.<PrivateDiscussionChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, chatService))
                                .to(privateDiscussionChannel.getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            } else {
                selectedChannelPin = chatService.getSelectedTradeChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicTradeChannel publicTradeChannel) {
                        chatMessagesPin = FxBindings.<PublicTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, chatService))
                                .to(publicTradeChannel.getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateTradeChannel privateTradeChannel) {
                        chatMessagesPin = FxBindings.<PrivateTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, chatService))
                                .to(privateTradeChannel.getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            }
       

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
            if (text != null && !text.isEmpty()) {
                Channel<? extends ChatMessage> channel = model.selectedChannel.get();
                ChatUserIdentity chatUserIdentity = chatUserService.getSelectedUserProfile().get();
                Optional<Quotation> quotation = quotedMessageBlock.getQuotation();
                if (channel instanceof PublicTradeChannel publicTradeChannel) {
                    chatService.publishTradeChatTextMessage(text, quotation, publicTradeChannel, chatUserIdentity);
                } else if (channel instanceof PublicDiscussionChannel publicDiscussionChannel) {
                    chatService.publishDiscussionChatMessage(text, quotation, publicDiscussionChannel, chatUserIdentity);
                } else if (channel instanceof PrivateTradeChannel privateTradeChannel) {
                    chatService.sendPrivateTradeChatMessage(text, quotation, privateTradeChannel);
                } else if (channel instanceof PrivateDiscussionChannel privateDiscussionChannel) {
                    chatService.sendPrivateDiscussionChatMessage(text, quotation, privateDiscussionChannel);
                }
                quotedMessageBlock.close();
            }
        }

        public void onMention(ChatUser chatUser) {
            mentionUser(chatUser);
        }

        public void onShowChatUserDetails(ChatMessage chatMessage) {
            chatService.findChatUser(chatMessage.getAuthorId()).ifPresent(author ->
                    model.showChatUserDetailsHandler.ifPresent(handler -> handler.accept(author)));
        }

        public void onOpenEmojiSelector(ChatMessage chatMessage) {

        }

        public void onReply(ChatMessage chatMessage) {
            if (!chatService.isMyMessage(chatMessage)) {
                quotedMessageBlock.reply(chatMessage);
            }
        }

        public void onOpenPrivateChannel(ChatMessage chatMessage) {
            if (chatService.isMyMessage(chatMessage)) {
                return;
            }
            chatService.findChatUser(chatMessage.getAuthorId()).ifPresent(this::createAndSelectPrivateChannel);
        }

        private void createAndSelectPrivateChannel(ChatUser peer) {
            if (model.isDiscussionsChat) {
                chatService.createPrivateDiscussionChannel(peer)
                        .ifPresent(chatService::selectTradeChannel);
            } else {
                createAndSelectPrivateTradeChannel(peer);
            }
        }

        private Optional<PrivateTradeChannel> createAndSelectPrivateTradeChannel(ChatUser peer) {
            Optional<PrivateTradeChannel> privateTradeChannel = chatService.createPrivateTradeChannel(peer);
            privateTradeChannel.ifPresent(chatService::selectTradeChannel);
            return privateTradeChannel;
        }

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        public void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            if (!chatService.isMyMessage(chatMessage)) {
                return;
            }
            if (chatMessage instanceof PublicTradeChatMessage marketChatMessage) {
                ChatUserIdentity chatUserIdentity = chatUserService.getSelectedUserProfile().get();
                chatService.publishEditedTradeChatMessage(marketChatMessage, editedText, chatUserIdentity);
            } else if (chatMessage instanceof PublicDiscussionChatMessage publicDiscussionChatMessage) {
                ChatUserIdentity chatUserIdentity = chatUserService.getSelectedUserProfile().get();
                chatService.publishEditedDiscussionChatMessage(publicDiscussionChatMessage, editedText, chatUserIdentity);
            }
            //todo editing private message not supported yet
        }

        public void onDeleteMessage(ChatMessage chatMessage) {
            if (chatService.isMyMessage(chatMessage)) {
                ChatUserIdentity chatUserIdentity = chatUserService.getSelectedUserProfile().get();
                if (chatMessage instanceof PublicTradeChatMessage marketChatMessage) {
                    chatService.deletePublicTradeChatMessage(marketChatMessage, chatUserIdentity);
                } else if (chatMessage instanceof PublicDiscussionChatMessage publicDiscussionChatMessage) {
                    chatService.deletePublicDiscussionChatMessage(publicDiscussionChatMessage, chatUserIdentity);
                }
                //todo delete private message
            }
        }

        public void onOpenMoreOptions(HBox reactionsBox, ChatMessage chatMessage, Runnable onClose) {
            if (chatMessage.equals(model.moreOptionsVisibleMessage.get())) {
                return;
            }
            model.moreOptionsVisibleMessage.set(chatMessage);
            List<BisqPopupMenuItem> items = new ArrayList<>();
            
            items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.copyMessage"), () -> {
                ClipboardUtil.copyToClipboard(chatMessage.getText());
            }));
            items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.copyLinkToMessage"), () -> {
                ClipboardUtil.copyToClipboard("???");  //todo implement url in chat message
            }));

            if (!chatService.isMyMessage(chatMessage)) {
                items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.ignoreUser"), () -> {
                    chatService.findChatUser(chatMessage.getAuthorId()).ifPresent(chatService::ignoreChatUser);
                }));
                items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.reportUser"), () -> {
                    chatService.findChatUser(chatMessage.getAuthorId()).ifPresent(author -> chatService.reportChatUser(author, ""));
                }));
            }

            BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
            menu.show(reactionsBox);
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

        public void fillUserMention(ChatUser user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z0-9]*$", "@" + user.getNickName() + " ");
            model.getTextInput().set(content);
            view.inputField.positionCaret(content.length());
        }

        public void fillChannelMention(Channel channel) {
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z0-9]*$", "#" + channel.getDisplayString() + " ");
            model.getTextInput().set(content);
            view.inputField.positionCaret(content.length());
        }

        public void onTakeOffer(PublicTradeChatMessage chatMessage) {
            if (model.isMyMessage(chatMessage) || chatMessage.getTradeChatOffer().isEmpty()) {
                return;
            }
            chatService.findChatUser(chatMessage.getAuthorId())
                    .flatMap(this::createAndSelectPrivateTradeChannel).ifPresent(privateTradeChannel -> {
                        String chatMessageText = chatMessage.getTradeChatOffer().get().getChatMessageText();
                        chatService.sendPrivateTradeChatMessage(
                                Res.get("satoshisquareapp.chat.takeOffer.takerRequest", chatMessageText),
                                Optional.empty(),
                                privateTradeChannel);
                    });
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final ChatUserService chatUserService;
        private final ReputationService reputationService;
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final StringProperty textInput = new SimpleStringProperty("");
        private final Predicate<ChatMessageListItem<? extends ChatMessage>> ignoredChatUserPredicate;
        private final boolean isDiscussionsChat;
        private final ObservableList<String> customTags = FXCollections.observableArrayList();
        private final BooleanProperty allowEditing = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private Optional<Consumer<ChatUser>> showChatUserDetailsHandler = Optional.empty();
        private final ObservableList<ChatUser> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<Channel> mentionableChannels = FXCollections.observableArrayList();

        private Model(ChatService chatService,
                      ChatUserService chatUserService,
                      ReputationService reputationService,
                      boolean isDiscussionsChat) {
            this.chatService = chatService;
            this.chatUserService = chatUserService;
            this.reputationService = reputationService;
            this.isDiscussionsChat = isDiscussionsChat;
            ignoredChatUserPredicate = item -> item.getAuthor().isPresent() &&
                    !chatService.getIgnoredChatUserIds().contains(item.getAuthor().get().getId());
            filteredChatMessages.setPredicate(ignoredChatUserPredicate);
            
            mentionableUsers.setAll(chatUserService.getMentionableChatUsers());
            mentionableChannels.setAll(chatService.getMentionableChannels());
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

        public ReputationScore getReputationScore(ChatUser author) {
            return reputationService.getReputationScore(author);
        }
    }


    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final ListView<ChatMessageListItem<? extends ChatMessage>> messagesListView;
        private final BisqTextArea inputField;
        private final ChatMentionPopupMenu<ChatUser> userMentionPopup;
        private final ChatMentionPopupMenu<Channel> channelMentionPopup;

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
            HBox.setHgrow(inputField, Priority.ALWAYS);

            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(ChatUser::getNickName);
            userMentionPopup.setSelectionHandler(controller::fillUserMention);
            
            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(Channel::getDisplayString);
            channelMentionPopup.setSelectionHandler(controller::fillChannelMention);

            // there will get added some controls for emojis so leave the box even its only 1 child yet
            bottomBox = Layout.hBoxWith(inputField);
            bottomBox.setAlignment(Pos.CENTER);
            VBox.setMargin(bottomBox, new Insets(0, 0, -10, 0));

            root.getChildren().addAll(messagesListView, quotedMessageBlock, bottomBox);

            messagesListener = c -> UIThread.runOnNextRenderFrame(() ->
                    messagesListView.scrollTo(messagesListView.getItems().size() - 1));
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

            userMentionPopup.setItems(model.mentionableUsers);
            userMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '@'),
                    inputField.textProperty()
            ));
            
            channelMentionPopup.setItems(model.mentionableChannels);
            channelMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '#'),
                    inputField.textProperty()
            ));
            
            model.getSortedChatMessages().addListener(messagesListener);
        }

        @Override
        protected void onViewDetached() {
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            inputField.setOnKeyPressed(null);
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();
            model.getSortedChatMessages().removeListener(messagesListener);
        }

        private Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                    return new ListCell<>() {
                        private final BisqTextArea editedMessageField;
                        private final Button takeOfferButton, saveEditButton, cancelEditButton;
                        private final Label emojiButton1, emojiButton2,
                                openEmojiSelectorButton, replyButton,
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
                        final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();

                        {
                            userNameLabel.setId("chat-user-name");
                            HBox userNameAndScore = Layout.hBoxWith(userNameLabel, reputationScoreDisplay);
                            userNameAndScore.setAlignment(Pos.CENTER_LEFT);
                            userNameAndScore.setPadding(new Insets(10, 0, -5, 0));

                            time.setId("chat-messages-date");
                            time.setPadding(new Insets(-6, 0, 0, 0));
                            time.setVisible(false);
                            time.setManaged(false);

                            emojiButton1 = Icons.getIcon(AwesomeIcon.THUMBS_UP_ALT);
                            emojiButton1.setUserData(":+1:");
                            emojiButton1.setCursor(Cursor.HAND);
                            emojiButton2 = Icons.getIcon(AwesomeIcon.THUMBS_DOWN_ALT);
                            emojiButton2.setUserData(":-1:");
                            emojiButton2.setCursor(Cursor.HAND);
                            openEmojiSelectorButton = Icons.getIcon(AwesomeIcon.SMILE);
                            openEmojiSelectorButton.setCursor(Cursor.HAND);
                            replyButton = Icons.getIcon(AwesomeIcon.REPLY);
                            replyButton.setCursor(Cursor.HAND);
                            pmButton = Icons.getIcon(AwesomeIcon.COMMENT_ALT);
                            pmButton.setCursor(Cursor.HAND);
                            editButton = Icons.getIcon(AwesomeIcon.EDIT);
                            editButton.setCursor(Cursor.HAND);
                            deleteButton = Icons.getIcon(AwesomeIcon.REMOVE_SIGN);
                            deleteButton.setCursor(Cursor.HAND);
                            moreOptionsButton = Icons.getIcon(AwesomeIcon.ELLIPSIS_HORIZONTAL);
                            moreOptionsButton.setCursor(Cursor.HAND);
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
                            VBox.setMargin(editControlsBox, new Insets(10, 0, 0, 0));

                            quotedMessageBox = new HBox();
                            quotedMessageBox.setSpacing(10);
                            quotedMessageBox.setVisible(false);
                            quotedMessageBox.setManaged(false);
                            VBox.setMargin(quotedMessageBox, new Insets(0, 0, 10, 0));

                            HBox.setMargin(emojiButton1, new Insets(0, 0, 0, 15));
                            HBox.setMargin(moreOptionsButton, new Insets(0, 15, 0, 0));
                            HBox.setMargin(verticalLine, new Insets(0, -10, 0, -10));
                            reactionsBox = Layout.hBoxWith(
                                    emojiButton1,
                                    emojiButton2,
                                    verticalLine,
                                    openEmojiSelectorButton,
                                    replyButton,
                                    pmButton,
                                    editButton,
                                    deleteButton,
                                    moreOptionsButton);
                            reactionsBox.setSpacing(30);
                            reactionsBox.setPadding(new Insets(5, 0, 5, 0));
                            reactionsBox.setVisible(false);
                            reactionsBox.setStyle("-fx-background-color: -bisq-grey-18; -fx-background-radius: 8 0 0 0");

                            HBox reactionsOuterBox = Layout.hBoxWith(Spacer.fillHBox(), reactionsBox);
                            VBox.setMargin(reactionsOuterBox, new Insets(-5, 0, 0, 0));

                            message.setAutoHeight(true);
                            HBox.setHgrow(message, Priority.ALWAYS);

                            takeOfferButton = new Button(Res.get("takeOffer"));
                            takeOfferButton.setVisible(false);
                            takeOfferButton.setManaged(false);

                            HBox.setMargin(takeOfferButton, new Insets(0, 10, 0, 0));
                            HBox messageAndTakeOfferButton = Layout.hBoxWith(message, takeOfferButton);
                            VBox.setMargin(messageAndTakeOfferButton, new Insets(-2, 0, 0, 0));
                            messageBox = Layout.vBoxWith(quotedMessageBox,
                                    messageAndTakeOfferButton,
                                    editedMessageField,
                                    editControlsBox,
                                    reactionsOuterBox);
                            VBox.setVgrow(messageBox, Priority.ALWAYS);

                            vBox = Layout.vBoxWith(userNameAndScore, messageBox);
                            HBox.setHgrow(vBox, Priority.ALWAYS);
                            VBox userIconTimeBox = Layout.vBoxWith(chatUserIcon, time);
                            HBox.setMargin(userIconTimeBox, new Insets(10, 0, 0, -10));
                            this.hBox = Layout.hBoxWith(userIconTimeBox, vBox);
                        }

                        private void hideHoverOverlay() {
                            time.setVisible(false);
                            time.setManaged(false);

                            reactionsBox.setVisible(false);
                            messageBox.setStyle("-fx-background-color: transparent");
                            setStyle("-fx-background-color: transparent");
                        }

                        @Override
                        public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {
                                Optional<Quotation> optionalQuotation = item.getQuotedMessage();
                                if (optionalQuotation.isPresent()) {
                                    quotedMessageBox.setVisible(true);
                                    quotedMessageBox.setManaged(true);
                                    Quotation quotation = optionalQuotation.get();
                                    if (quotation.nickName() != null &&
                                            quotation.nym() != null &&
                                            quotation.pubKeyHash() != null &&
                                            quotation.message() != null) {
                                        Region verticalLine = new Region();
                                        verticalLine.setStyle("-fx-background-color: -bisq-grey-9");
                                        verticalLine.setMinWidth(3);
                                        verticalLine.setMinHeight(25);
                                        HBox.setMargin(verticalLine, new Insets(0, 0, 0, 5));

                                        quotedMessageField.setText(quotation.message());
                                        quotedMessageField.setStyle("-fx-fill: -bisq-grey-9");

                                        Label userName = new Label(quotation.getUserName());
                                        userName.setPadding(new Insets(4, 0, 0, 0));
                                        userName.setStyle("-fx-text-fill: -bisq-grey-9");

                                        ImageView roboIconImageView = new ImageView();
                                        roboIconImageView.setFitWidth(25);
                                        roboIconImageView.setFitHeight(25);
                                        Image image = RoboHash.getImage(quotation.pubKeyHash());
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

                                ChatMessage chatMessage = item.getChatMessage();
                                if (!model.isMyMessage(chatMessage) &&
                                        chatMessage instanceof PublicTradeChatMessage marketChatMessage &&
                                        marketChatMessage.getTradeChatOffer().isPresent()) {
                                    takeOfferButton.setVisible(true);
                                    takeOfferButton.setManaged(true);
                                    takeOfferButton.setOnAction(e -> controller.onTakeOffer(marketChatMessage));

                                } else {
                                    takeOfferButton.setVisible(false);
                                    takeOfferButton.setManaged(false);
                                }

                                message.setText(item.getMessage());
                                message.setStyleSpans(0, KeyWordDetection.getStyleSpans(item.getMessage(), model.getCustomTags()));

                                time.setText(item.getTime());

                                saveEditButton.setOnAction(e -> {
                                    controller.onSaveEditedMessage(chatMessage, editedMessageField.getText());
                                    onCloseEditMessage();
                                });
                                cancelEditButton.setOnAction(e -> onCloseEditMessage());

                                dateTooltip = new Tooltip(item.getDate());
                                Tooltip.install(time, dateTooltip);

                                item.getAuthor().ifPresent(author -> {
                                    userNameLabel.setText(author.getUserName());
                                    userNameLabel.setOnMouseClicked(e -> controller.onMention(author));

                                    chatUserIcon.setCursor(Cursor.HAND);
                                    chatUserIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(chatMessage));
                                    chatUserIcon.setChatUser(author, model.getChatUserService());
                                    Tooltip.install(chatUserIcon, new Tooltip(author.getTooltipString()));

                                    reputationScoreDisplay.applyReputationScore(model.getReputationScore(author));
                                });

                                setOnMouseEntered(e -> {
                                    if (model.moreOptionsVisibleMessage.get() != null) {
                                        return;
                                    }
                                    time.setVisible(true);
                                    time.setManaged(true);

                                    reactionsBox.setVisible(true);
                                    messageBox.setStyle("-fx-background-color: -bisq-grey-2");
                                    setStyle("-fx-background-color: -bisq-grey-2;");
                                });
                                setOnMouseExited(e -> {
                                    if (model.moreOptionsVisibleMessage.get() == null) {
                                        hideHoverOverlay();
                                    }
                                });

                                emojiButton1.setOnMouseClicked(e -> controller.onAddEmoji((String) emojiButton1.getUserData()));
                                emojiButton2.setOnMouseClicked(e -> controller.onAddEmoji((String) emojiButton2.getUserData()));
                                openEmojiSelectorButton.setOnMouseClicked(e -> controller.onOpenEmojiSelector(chatMessage));
                                replyButton.setOnMouseClicked(e -> controller.onReply(chatMessage));
                                pmButton.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
                                editButton.setOnMouseClicked(e -> onEditMessage(item));
                                deleteButton.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
                                moreOptionsButton.setOnMouseClicked(e -> controller.onOpenMoreOptions(reactionsBox, chatMessage, () -> {
                                    hideHoverOverlay();
                                    model.moreOptionsVisibleMessage.set(null);
                                }));

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
                                emojiButton1.setOnMouseClicked(null);
                                emojiButton2.setOnMouseClicked(null);
                                openEmojiSelectorButton.setOnMouseClicked(null);
                                replyButton.setOnMouseClicked(null);
                                pmButton.setOnMouseClicked(null);
                                editButton.setOnMouseClicked(null);
                                deleteButton.setOnMouseClicked(null);
                                moreOptionsButton.setOnMouseClicked(null);
                                saveEditButton.setOnAction(null);
                                cancelEditButton.setOnAction(null);
                                editedMessageField.setOnKeyPressed(null);
                                takeOfferButton.setOnAction(null);

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
        private final Optional<Quotation> quotedMessage;
        private final Optional<ChatUser> author;
        private final String nym;
        private final String nickName;

        public ChatMessageListItem(T chatMessage, ChatService chatService) {
            this.chatMessage = chatMessage;
            
            if (chatMessage instanceof PrivateTradeChatMessage privateTradeChatMessage) {
                author = Optional.of(privateTradeChatMessage.getAuthor());
            } else if (chatMessage instanceof PrivateDiscussionChatMessage privateDiscussionChatMessage) {
                author = Optional.of(privateDiscussionChatMessage.getAuthor());
            } else {
                author = chatService.findChatUser(chatMessage.getAuthorId());
            }
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            quotedMessage = chatMessage.getQuotation();
            time = TimeFormatter.formatTime(new Date(chatMessage.getDate()));
            date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()));

            nym = author.map(ChatUser::getNym).orElse("");
            nickName = author.map(ChatUser::getNickName).orElse("");
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
                    StringUtils.containsIgnoreCase(nym, filterString) ||
                    StringUtils.containsIgnoreCase(nickName, filterString) ||
                    StringUtils.containsIgnoreCase(date, filterString);
        }
    }
}