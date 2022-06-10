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
import bisq.social.user.reputation.ReputationService;
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

        private Controller(ChatService chatService,
                           ChatUserService chatUserService,
                           ReputationService reputationService,
                           boolean isDiscussionsChat) {
            this.chatService = chatService;
            this.chatUserService = chatUserService;
            quotedMessageBlock = new QuotedMessageBlock(chatService);
            chatMessagesListView = new ChatMessagesListView(chatService,
                    chatUserService,
                    reputationService,
                    this::mentionUser,
                    this::showChatUserDetails,
                    this::onReply,
                    isDiscussionsChat);

            model = new Model(chatService, chatUserService, isDiscussionsChat);
            view = new View(model, this, chatMessagesListView.getRoot(), quotedMessageBlock.getRoot());
        }

        @Override
        public void onActivate() {
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
            Navigation.navigateTo(NavigationTarget.ONBOARDING_DIRECTION);
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

        public void fillChannelMention(Channel channel) {
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z0-9]*$", "#" + channel.getDisplayString() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final ChatUserService chatUserService;
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final boolean isDiscussionsChat;
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private Optional<Consumer<ChatUser>> showChatUserDetailsHandler = Optional.empty();
        private final ObservableList<ChatUser> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<Channel<?>> mentionableChannels = FXCollections.observableArrayList();

        private Model(ChatService chatService,
                      ChatUserService chatUserService,
                      boolean isDiscussionsChat) {
            this.chatService = chatService;
            this.chatUserService = chatUserService;
            this.isDiscussionsChat = isDiscussionsChat;

            mentionableUsers.setAll(chatUserService.getMentionableChatUsers());
            mentionableChannels.setAll(chatService.getMentionableChannels());
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

         /*   messagesListView = new ListView<>(model.getSortedChatMessages());
            messagesListView.getStyleClass().add("chat-messages-list-view");
            Label placeholder = new Label(Res.get("table.placeholder.noData"));
            messagesListView.setPlaceholder(placeholder);
            messagesListView.setCellFactory(getCellFactory());
            VBox.setMargin(messagesListView, new Insets(0, 24, 0, 24));

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            messagesListView.setSelectionModel(new NoSelectionModel<>());*/

            inputField = new BisqTextArea();
            inputField.setId("chat-input-field");
            inputField.setPromptText(Res.get("social.chat.input.prompt"));

            sendButton = new Button("", ImageUtil.getImageViewById("chat-send"));
            sendButton.setId("chat-messages-send-button");
            sendButton.setPadding(new Insets(5));
            sendButton.setMinWidth(31);
            sendButton.setMaxWidth(31);
            // sendButton.setText(Res.get("send"));

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

         /*   messagesListener = c -> UIThread.runOnNextRenderFrame(() ->
                    messagesListView.scrollTo(messagesListView.getItems().size() - 1));*/
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

            sendButton.setOnAction(event -> {
                controller.onSendMessage(StringUtils.trimTrailingLinebreak(inputField.getText()));
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

            // model.getSortedChatMessages().addListener(messagesListener);
        }

        @Override
        protected void onViewDetached() {
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
            createOfferButton.setOnAction(null);
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();
            // model.getSortedChatMessages().removeListener(messagesListener);
        }

     /*   private Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                    return new ListCell<>() {
                        private final AnchorPane anchorPane;
                        private final VBox reputationBox;
                        private final BisqTextArea editInputField;
                        private final Button actionButton, saveEditButton, cancelEditButton;
                        private final Label emojiButton1, emojiButton2,
                                openEmojiSelectorButton, replyButton,
                                pmButton, editButton, deleteButton, moreOptionsButton;
                        private final Label userNameLabel = new Label();
                        private final Label dateTime = new Label();
                        private final Label message = new Label();
                        private final Text quotedMessageField = new Text();
                        private final HBox hBox, messageContainer, reactionsBox, editControlsBox, quotedMessageBox;
                        private final ChatUserIcon chatUserIcon = new ChatUserIcon(42);
                        private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
                        private Subscription widthSubscription, messageWidthSubscription;

                        {
                            userNameLabel.setId("chat-user-name");
                            dateTime.setId("chat-messages-date");

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
                            HBox.setMargin(verticalLine, new Insets(0, -10, 0, -10));
                            verticalLine.setId("chat-message-reactions-separator");

                            editInputField = new BisqTextArea();
                            editInputField.setId("chat-messages-edit-text-area");
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);

                            saveEditButton = new Button(Res.get("shared.save"));
                            saveEditButton.setDefaultButton(true);
                            cancelEditButton = new Button(Res.get("shared.cancel"));
                            editControlsBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
                            editControlsBox.setVisible(false);
                            editControlsBox.setManaged(false);

                            quotedMessageBox = new HBox();
                            quotedMessageBox.setSpacing(10);
                            quotedMessageBox.setVisible(false);
                            quotedMessageBox.setManaged(false);
                            VBox.setMargin(quotedMessageBox, new Insets(0, 0, 10, 0));

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
                            reactionsBox.setSpacing(20);
                            reactionsBox.setPadding(new Insets(0, 15, 5, 15));
                            reactionsBox.setVisible(false);

                            message.setId("chat-messages-message");
                            message.setWrapText(true);

                            actionButton = new Button();
                            actionButton.setVisible(false);
                            actionButton.setManaged(false);
                            HBox.setMargin(actionButton, new Insets(0, 10, 0, 0));

                            Label reputationLabel = new Label(Res.get("reputation").toUpperCase());
                            reputationLabel.getStyleClass().add("bisq-text-7");
                            reputationBox = new VBox(4, reputationLabel, reputationScoreDisplay);
                            reputationBox.setAlignment(Pos.CENTER_LEFT);
                            HBox.setHgrow(message, Priority.NEVER);
                            messageContainer = Layout.hBoxWith(message, Spacer.fillHBox(), reputationBox, actionButton);
                            messageContainer.setPadding(new Insets(15));
                            messageContainer.setAlignment(Pos.CENTER_LEFT);

                            VBox.setVgrow(editInputField, Priority.ALWAYS);
                            VBox messageBox = Layout.vBoxWith(quotedMessageBox, messageContainer, editInputField);

                            anchorPane = new AnchorPane();
                            AnchorPane.setTopAnchor(messageBox, 0.0);
                            AnchorPane.setLeftAnchor(messageBox, 0.0);
                            AnchorPane.setRightAnchor(messageBox, 0.0);
                            AnchorPane.setBottomAnchor(messageBox, 0.0);

                            AnchorPane.setRightAnchor(reactionsBox, 0.0);
                            AnchorPane.setBottomAnchor(reactionsBox, -10.0);
                            AnchorPane.setRightAnchor(editControlsBox, 10.0);
                            AnchorPane.setBottomAnchor(editControlsBox, 0.0);
                            anchorPane.getChildren().addAll(messageBox, reactionsBox, editControlsBox);

                            HBox userInfoBox = new HBox(5, userNameLabel, dateTime);
                            VBox vBox = new VBox(0, userInfoBox, anchorPane);
                            HBox.setHgrow(vBox, Priority.ALWAYS);
                            hBox = Layout.hBoxWith(chatUserIcon, vBox);
                        }

                        private void hideHoverOverlay() {
                            reactionsBox.setVisible(false);
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
                                            quotation.proofOfWork() != null &&
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
                                        Image image = RoboHash.getImage(quotation.proofOfWork().getPayload());
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
                                boolean isOfferMessage = chatMessage instanceof PublicTradeChatMessage publicTradeChatMessage &&
                                        publicTradeChatMessage.getTradeChatOffer().isPresent();

                                if (isOfferMessage) {
                                    actionButton.setVisible(true);
                                    actionButton.setManaged(true);
                                    if (model.isMyMessage(chatMessage)) {
                                        actionButton.setText(Res.get("deleteOffer"));
                                        actionButton.getStyleClass().remove("default-button");
                                        actionButton.getStyleClass().add("red-button");
                                        actionButton.setOnAction(e -> controller.onDeleteMyOffer((PublicTradeChatMessage) chatMessage));
                                    } else {
                                        actionButton.setText(Res.get("takeOffer"));
                                        actionButton.getStyleClass().remove("red-button");
                                        actionButton.getStyleClass().add("default-button");
                                        actionButton.setOnAction(e -> controller.onTakeOffer((PublicTradeChatMessage) chatMessage));
                                    }
                                } else {
                                    actionButton.setVisible(false);
                                    actionButton.setManaged(false);
                                }

                                if (isOfferMessage) {
                                    VBox.setMargin(messageContainer, new Insets(15, 0, 25, 0));
                                    HBox.setMargin(reputationBox, new Insets(0, 10, 0, 0));
                                } else {
                                    VBox.setMargin(messageContainer, new Insets(-10, 0, 0, 0));
                                    HBox.setMargin(reputationBox, new Insets(-30, 10, 0, 0));
                                }
                                Layout.toggleStyleClass(messageContainer, "chat-offer-box", isOfferMessage);

                                message.setText(item.getMessage());

                                dateTime.setText(item.getDate());

                                saveEditButton.setOnAction(e -> {
                                    controller.onSaveEditedMessage(chatMessage, editInputField.getText());
                                    onCloseEditMessage();
                                });
                                cancelEditButton.setOnAction(e -> onCloseEditMessage());

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

                                    if (!editInputField.isVisible()) {
                                        reactionsBox.setVisible(true);
                                    }
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

                                // Hack to get message wrapped and filled space
                                messageWidthSubscription = EasyBind.subscribe(reputationBox.widthProperty(),
                                        width -> {
                                            if (reputationBox.getWidth() > 0) {
                                                message.setPrefWidth(root.getWidth() - actionButton.getWidth() - reputationBox.getWidth() - 220);
                                            }
                                        });
                                setGraphic(hBox);
                            } else {
                                if (widthSubscription != null) {
                                    widthSubscription.unsubscribe();
                                }
                                if (messageWidthSubscription != null) {
                                    messageWidthSubscription.unsubscribe();
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
                                editInputField.setOnKeyPressed(null);
                                actionButton.setOnAction(null);

                                setGraphic(null);
                            }
                        }

                        private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                            reactionsBox.setVisible(false);
                            editInputField.setVisible(true);
                            editInputField.setManaged(true);
                            editInputField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                            editInputField.setScrollHideThreshold(200);
                            editInputField.requestFocus();
                            editInputField.positionCaret(message.getText().length());
                            editControlsBox.setVisible(true);
                            editControlsBox.setManaged(true);
                            message.setVisible(false);
                            message.setManaged(false);

                            ChatMessage chatMessage = item.getChatMessage();
                            boolean isOfferMessage = chatMessage instanceof PublicTradeChatMessage publicTradeChatMessage &&
                                    publicTradeChatMessage.getTradeChatOffer().isPresent();

                            if (isOfferMessage) {

                            }
                            if (isOfferMessage) {
                                VBox.setMargin(editInputField, new Insets(-88, 0, 40, 5));
                                AnchorPane.setBottomAnchor(editControlsBox, -10.0);
                                VBox.setMargin(anchorPane, new Insets(0, 0, 15, 0));
                            } else {
                                VBox.setMargin(editInputField, new Insets(-38, 0, 15, 5));
                                AnchorPane.setBottomAnchor(editControlsBox, 0.0);
                                VBox.setMargin(anchorPane, new Insets(0, 0, 0, 0));
                            }

                            editInputField.setOnKeyPressed(event -> {
                                if (event.getCode() == KeyCode.ENTER) {
                                    event.consume();
                                    if (event.isShiftDown()) {
                                        editInputField.appendText(System.getProperty("line.separator"));
                                    } else if (!editInputField.getText().isEmpty()) {
                                        controller.onSaveEditedMessage(item.getChatMessage(),
                                                StringUtils.trimTrailingLinebreak(editInputField.getText()));
                                        onCloseEditMessage();
                                    }
                                }
                            });
                        }

                        private void onCloseEditMessage() {
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);
                            editControlsBox.setVisible(false);
                            editControlsBox.setManaged(false);
                            message.setVisible(true);
                            message.setManaged(true);
                            editInputField.setOnKeyPressed(null);
                            AnchorPane.setBottomAnchor(editControlsBox, 0.0);
                            VBox.setMargin(anchorPane, new Insets(0, 0, 0, 0));
                        }
                    };
                }
            };
        }
    }*/

   /* @Slf4j
    @Getter
    @EqualsAndHashCode
    public static class ChatMessageListItem<T extends ChatMessage> implements Comparable<ChatMessageListItem<T>>, FilteredListItem {
        private final T chatMessage;
        private final String message;
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
            date = DateFormatter.formatDateTimeV2(new Date(chatMessage.getDate()));

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
    }*/
    }
}