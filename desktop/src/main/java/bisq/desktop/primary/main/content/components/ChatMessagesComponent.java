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

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.i18n.Res;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.*;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.Quotation;
import bisq.social.user.ChatUser;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesComponent {
    private final Controller controller;

    public ChatMessagesComponent(DefaultApplicationService applicationService, boolean isDiscussionsChat) {
        controller = new Controller(applicationService, isDiscussionsChat);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void mentionUser(ChatUser chatUser) {
        controller.mentionUser(chatUser);
    }

    public FilteredList<ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.chatMessagesListView.getFilteredChatMessages();
    }

    public void setOnShowChatUserDetails(Consumer<ChatUser> handler) {
        controller.model.showChatUserDetailsHandler = Optional.of(handler);
    }

    public void openPrivateChannel(ChatUser peer) {
        controller.createAndSelectPrivateChannel(peer);
    }

    public void refreshMessages() {
        controller.chatMessagesListView.refreshMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final ChatUserService chatUserService;
        private final QuotedMessageBlock quotedMessageBlock;
        private final ChatMessagesListView chatMessagesListView;
        private Pin selectedChannelPin;

        private Controller(DefaultApplicationService applicationService,
                           boolean isDiscussionsChat) {
            this.chatService = applicationService.getChatService();
            this.chatUserService = applicationService.getChatUserService();
            quotedMessageBlock = new QuotedMessageBlock(chatService);
            chatMessagesListView = new ChatMessagesListView(applicationService,
                    this::mentionUser,
                    this::showChatUserDetails,
                    this::onReply,
                    isDiscussionsChat,
                    false,
                    false,
                    false);

            model = new Model(isDiscussionsChat);
            view = new View(model, this, chatMessagesListView.getRoot(), quotedMessageBlock.getRoot());
        }

        @Override
        public void onActivate() {
            model.mentionableUsers.setAll(chatUserService.getMentionableChatUsers());
            model.mentionableChannels.setAll(chatService.getMentionableChannels());

            if (model.isDiscussionsChat) {
                selectedChannelPin = chatService.getSelectedDiscussionChannel().addObserver(model.selectedChannel::set);
            } else {
                selectedChannelPin = chatService.getSelectedTradeChannel().addObserver(model.selectedChannel::set);
            }
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        void onSendMessage(String text) {
            if (text != null && !text.isEmpty()) {
                Channel<? extends ChatMessage> channel = model.selectedChannel.get();
                ChatUserIdentity chatUserIdentity = chatUserService.getSelectedUserProfile().get();
                checkNotNull(chatUserIdentity, "chatUserIdentity must not be null at onSendMessage");
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

        public void showChatUserDetails(ChatMessage chatMessage) {
            chatService.findChatUser(chatMessage.getAuthorId()).ifPresent(author ->
                    model.showChatUserDetailsHandler.ifPresent(handler -> handler.accept(author)));
        }

        public void onReply(ChatMessage chatMessage) {
            if (!chatService.isMyMessage(chatMessage)) {
                quotedMessageBlock.reply(chatMessage);
            }
        }

        private void onCreateOffer() {
            Navigation.navigateTo(NavigationTarget.CREATE_OFFER);
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
            //todo
            view.inputField.positionCaret(content.length());
        }

        public void fillChannelMention(Channel<?> channel) {
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z0-9]*$", "#" + channel.getDisplayString() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final boolean isDiscussionsChat;
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<ChatUser> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<Channel<?>> mentionableChannels = FXCollections.observableArrayList();
        private Optional<Consumer<ChatUser>> showChatUserDetailsHandler = Optional.empty();

        private Model(boolean isDiscussionsChat) {
            this.isDiscussionsChat = isDiscussionsChat;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final BisqTextArea inputField;
        private final Button sendButton, createOfferButton;
        private final ChatMentionPopupMenu<ChatUser> userMentionPopup;
        private final ChatMentionPopupMenu<Channel<?>> channelMentionPopup;

        private View(Model model, Controller controller, Pane messagesListView, Pane quotedMessageBlock) {
            super(new VBox(), model, controller);

            inputField = new BisqTextArea();
            inputField.setId("chat-input-field");
            inputField.setPromptText(Res.get("social.chat.input.prompt"));

            sendButton = new Button("", ImageUtil.getImageViewById("chat-send"));
            sendButton.setId("chat-messages-send-button");
            sendButton.setPadding(new Insets(5));
            sendButton.setMinWidth(31);
            sendButton.setMaxWidth(31);

            StackPane stackPane = new StackPane(inputField, sendButton);
            StackPane.setAlignment(inputField, Pos.CENTER_LEFT);
            StackPane.setAlignment(sendButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(sendButton, new Insets(0, 10, 0, 0));

            createOfferButton = new Button(Res.get("satoshisquareapp.chat.createOffer.button"));
            createOfferButton.setDefaultButton(true);
            createOfferButton.setMinWidth(140);

            HBox.setMargin(createOfferButton, new Insets(0, 0, 0, 0));
            HBox.setHgrow(createOfferButton, Priority.ALWAYS);
            HBox.setHgrow(stackPane, Priority.ALWAYS);
            //  private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;
            HBox bottomBox = new HBox(10, stackPane, createOfferButton);
            bottomBox.getStyleClass().add("bg-grey-5");
            bottomBox.setAlignment(Pos.CENTER);
            bottomBox.setPadding(new Insets(14, 24, 14, 24));

            VBox.setVgrow(messagesListView, Priority.ALWAYS);
            VBox.setMargin(quotedMessageBlock, new Insets(0, 24, 0, 24));
            root.getChildren().addAll(messagesListView, quotedMessageBlock, bottomBox);

            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(ChatUser::getNickName);
            userMentionPopup.setSelectionHandler(controller::fillUserMention);

            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(Channel::getDisplayString);
            channelMentionPopup.setSelectionHandler(controller::fillChannelMention);
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
                        controller.onSendMessage(inputField.getText().trim());
                        inputField.clear();
                    }
                }
            });

            sendButton.setOnAction(event -> {
                controller.onSendMessage(inputField.getText().trim());
                inputField.clear();
            });
            createOfferButton.setOnAction(e -> controller.onCreateOffer());

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
        }

        @Override
        protected void onViewDetached() {
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
            createOfferButton.setOnAction(null);
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();
        }
    }
}