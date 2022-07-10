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
import bisq.desktop.components.controls.BisqTextArea;
import bisq.i18n.Res;
import bisq.settings.DontShowAgainService;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.*;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.Quotation;
import bisq.identity.profile.PublicUserProfile;
import bisq.identity.profile.UserProfile;
import bisq.social.user.ChatUserService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
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

import static bisq.settings.DontShowAgainKey.TRADE_GUIDE_BOX;
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

    public void mentionUser(PublicUserProfile publicUserProfile) {
        controller.mentionUser(publicUserProfile);
    }

    public FilteredList<ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.chatMessagesListView.getFilteredChatMessages();
    }

    public void setOnShowChatUserDetails(Consumer<PublicUserProfile> handler) {
        controller.model.showChatUserDetailsHandler = Optional.of(handler);
    }

    public void openPrivateChannel(PublicUserProfile peer) {
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
            this.chatService = applicationService.getSocialService().getChatService();
            this.chatUserService = applicationService.getSocialService().getChatUserService();
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
                selectedChannelPin = chatService.getSelectedTradeChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    model.isTradeGuideBoxVisible.set(displayTradeGuileBox());
                });
            }

            model.isTradeGuideBoxVisible.set(displayTradeGuileBox());

            DontShowAgainService.getUpdateFlag().addObserver(e -> model.isTradeGuideBoxVisible.set(displayTradeGuileBox()));
        }

        private boolean displayTradeGuileBox() {
            return DontShowAgainService.showAgain(TRADE_GUIDE_BOX) &&
                    model.getSelectedChannel().get() instanceof PrivateTradeChannel;
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
                UserProfile userProfile = chatUserService.getSelectedChatUserIdentity().get();
                checkNotNull(userProfile, "chatUserIdentity must not be null at onSendMessage");
                Optional<Quotation> quotation = quotedMessageBlock.getQuotation();
                if (channel instanceof PublicTradeChannel) {
                    chatService.publishTradeChatTextMessage(text, quotation, (PublicTradeChannel) channel, userProfile);
                } else if (channel instanceof PublicDiscussionChannel) {
                    chatService.publishDiscussionChatMessage(text, quotation, (PublicDiscussionChannel) channel, userProfile);
                } else if (channel instanceof PrivateTradeChannel) {
                    chatService.sendPrivateTradeChatMessage(text, quotation, (PrivateTradeChannel) channel);
                } else if (channel instanceof PrivateDiscussionChannel) {
                    chatService.sendPrivateDiscussionChatMessage(text, quotation, (PrivateDiscussionChannel) channel);
                }
                quotedMessageBlock.close();
            }
        }

        public void onReply(ChatMessage chatMessage) {
            if (!chatService.isMyMessage(chatMessage)) {
                quotedMessageBlock.reply(chatMessage);
            }
        }

        private void createAndSelectPrivateChannel(PublicUserProfile peer) {
            if (model.isDiscussionsChat) {
                chatService.createPrivateDiscussionChannel(peer)
                        .ifPresent(chatService::selectTradeChannel);
            } else {
                createAndSelectPrivateTradeChannel(peer);
            }
        }

        private Optional<PrivateTradeChannel> createAndSelectPrivateTradeChannel(PublicUserProfile peer) {
            Optional<PrivateTradeChannel> privateTradeChannel = chatService.createPrivateTradeChannel(peer);
            privateTradeChannel.ifPresent(chatService::selectTradeChannel);
            return privateTradeChannel;
        }

        private void showChatUserDetails(ChatMessage chatMessage) {
            chatService.findChatUser(chatMessage.getAuthorId()).ifPresent(author ->
                    model.showChatUserDetailsHandler.ifPresent(handler -> handler.accept(author)));
        }

        private void mentionUser(PublicUserProfile publicUserProfile) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + publicUserProfile.getUserName() + " ");
        }

        public void fillUserMention(PublicUserProfile user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getNickName() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        public void fillChannelMention(Channel<?> channel) {
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + channel.getDisplayString() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        public void onCloseTradeGuideBox() {
            model.getIsTradeGuideBoxVisible().set(false);
            DontShowAgainService.dontShowAgain(TRADE_GUIDE_BOX);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final boolean isDiscussionsChat;
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<PublicUserProfile> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<Channel<?>> mentionableChannels = FXCollections.observableArrayList();
        private Optional<Consumer<PublicUserProfile>> showChatUserDetailsHandler = Optional.empty();
        private final BooleanProperty isTradeGuideBoxVisible = new SimpleBooleanProperty();

        private Model(boolean isDiscussionsChat) {
            this.isDiscussionsChat = isDiscussionsChat;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final BisqTextArea inputField;
        private final Button sendButton;
        private final ChatMentionPopupMenu<PublicUserProfile> userMentionPopup;
        private final ChatMentionPopupMenu<Channel<?>> channelMentionPopup;
        private final TradeGuideBox tradeGuideBox;
        private final Button closeTradeGuideBoxButton;

        private View(Model model, Controller controller, Pane messagesListView, Pane quotedMessageBlock) {
            super(new VBox(), model, controller);

            tradeGuideBox = new TradeGuideBox();
            closeTradeGuideBoxButton = tradeGuideBox.getCloseButton();

            inputField = new BisqTextArea();
            inputField.setId("chat-input-field");
            inputField.setPromptText(Res.get("social.chat.input.prompt"));

            sendButton = new Button("", ImageUtil.getImageViewById("chat-send"));
            sendButton.setId("chat-messages-send-button");
            sendButton.setPadding(new Insets(5));
            sendButton.setMinWidth(31);
            sendButton.setMaxWidth(31);

            StackPane bottomBoxStackPane = new StackPane(inputField, sendButton);
            StackPane.setAlignment(inputField, Pos.CENTER_LEFT);
            StackPane.setAlignment(sendButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(sendButton, new Insets(0, 10, 0, 0));

            HBox.setHgrow(bottomBoxStackPane, Priority.ALWAYS);
            HBox bottomBox = new HBox(10, bottomBoxStackPane);
            bottomBox.getStyleClass().add("bg-grey-5");
            bottomBox.setAlignment(Pos.CENTER);
            bottomBox.setPadding(new Insets(14, 24, 14, 24));

            VBox.setVgrow(messagesListView, Priority.ALWAYS);
            VBox.setVgrow(tradeGuideBox, Priority.SOMETIMES);
            VBox.setMargin(tradeGuideBox, new Insets(0, 24, 24, 24));
            VBox.setMargin(quotedMessageBlock, new Insets(0, 24, 0, 24));
            root.getChildren().addAll(tradeGuideBox, messagesListView, quotedMessageBlock, bottomBox);

            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(PublicUserProfile::getNickName);
            userMentionPopup.setSelectionHandler(controller::fillUserMention);

            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(Channel::getDisplayString);
            channelMentionPopup.setSelectionHandler(controller::fillChannelMention);
        }

        @Override
        protected void onViewAttached() {
            tradeGuideBox.visibleProperty().bind(model.getIsTradeGuideBoxVisible());
            tradeGuideBox.managedProperty().bind(model.getIsTradeGuideBoxVisible());
            inputField.textProperty().bindBidirectional(model.getTextInput());

            userMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '@'),
                    inputField.textProperty()
            ));
            channelMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '#'),
                    inputField.textProperty()
            ));

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
            closeTradeGuideBoxButton.setOnAction(e -> controller.onCloseTradeGuideBox());

            userMentionPopup.setItems(model.mentionableUsers);
            channelMentionPopup.setItems(model.mentionableChannels);
        }

        @Override
        protected void onViewDetached() {
            tradeGuideBox.visibleProperty().unbind();
            tradeGuideBox.managedProperty().unbind();
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();

            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
            closeTradeGuideBoxButton.setOnAction(null);
        }
    }
}