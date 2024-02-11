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

package bisq.desktop.main.content.components.chatMessages;

import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.pub.PublicChatMessage;
import bisq.desktop.common.Icons;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

final class ChatMessageListCellFactory implements Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> {
    private final ChatMessagesListView.Controller controller;
    private final ChatMessagesListView.Model model;

    public ChatMessageListCellFactory(ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        this.controller = controller;
        this.model = model;
    }

    @Override
    public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
        return new ListCell<>() {
            private final static double CHAT_BOX_MAX_WIDTH = 1200;
            private final static double CHAT_MESSAGE_BOX_MAX_WIDTH = 630;
            private final String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");

            private final ReputationScoreDisplay reputationScoreDisplay;
            private final Button takeOfferButton, removeOfferButton;
            private final Label message, userName, dateTime, replyIcon, pmIcon, editIcon, deleteIcon, copyIcon,
                    moreOptionsIcon, supportedLanguages;
            private final Label deliveryState;
            private final Label quotedMessageField = new Label();
            private final BisqTextArea editInputField;
            private final Button saveEditButton, cancelEditButton;
            private final VBox mainVBox, quotedMessageVBox;
            private final HBox cellHBox, messageHBox, messageBgHBox, reactionsHBox, editButtonsHBox;
            private final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
            private final Set<Subscription> subscriptions = new HashSet<>();

            {
                userName = new Label();
                userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");

                deliveryState = new Label();
                deliveryState.setCursor(Cursor.HAND);
                deliveryState.setTooltip(new BisqTooltip(true));

                dateTime = new Label();
                dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");

                reputationScoreDisplay = new ReputationScoreDisplay();
                takeOfferButton = new Button(Res.get("offer.takeOffer"));

                removeOfferButton = new Button(Res.get("offer.deleteOffer"));
                removeOfferButton.getStyleClass().addAll("red-small-button", "no-background");

                // quoted message
                quotedMessageField.setWrapText(true);
                quotedMessageVBox = new VBox(5);
                quotedMessageVBox.setVisible(false);
                quotedMessageVBox.setManaged(false);

                // HBox for message reputation vBox and action button
                message = new Label();
                message.setWrapText(true);
                message.setPadding(new Insets(10));
                message.getStyleClass().addAll("text-fill-white", "normal-text", "font-default");


                // edit
                editInputField = new BisqTextArea();
                //editInputField.getStyleClass().addAll("text-fill-white", "font-size-13", "font-default");
                editInputField.setId("chat-messages-edit-text-area");
                editInputField.setMinWidth(150);
                editInputField.setVisible(false);
                editInputField.setManaged(false);

                // edit buttons
                saveEditButton = new Button(Res.get("action.save"));
                saveEditButton.setDefaultButton(true);
                cancelEditButton = new Button(Res.get("action.cancel"));

                editButtonsHBox = new HBox(15, Spacer.fillHBox(), cancelEditButton, saveEditButton);
                editButtonsHBox.setVisible(false);
                editButtonsHBox.setManaged(false);

                messageBgHBox = new HBox(15);
                messageBgHBox.setAlignment(Pos.CENTER_LEFT);
                messageBgHBox.setMaxWidth(CHAT_MESSAGE_BOX_MAX_WIDTH);

                // Reactions box
                replyIcon = getIconWithToolTip(AwesomeIcon.REPLY, Res.get("chat.message.reply"));
                pmIcon = getIconWithToolTip(AwesomeIcon.COMMENT_ALT, Res.get("chat.message.privateMessage"));
                editIcon = getIconWithToolTip(AwesomeIcon.EDIT, Res.get("action.edit"));
                HBox.setMargin(editIcon, new Insets(1, 0, 0, 0));
                copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
                deleteIcon = getIconWithToolTip(AwesomeIcon.REMOVE_SIGN, Res.get("action.delete"));
                moreOptionsIcon = getIconWithToolTip(AwesomeIcon.ELLIPSIS_HORIZONTAL, Res.get("chat.message.moreOptions"));
                supportedLanguages = new Label();

                reactionsHBox = new HBox(20);

                reactionsHBox.setVisible(false);

                HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
                messageHBox = new HBox();

                VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
                VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));
                VBox.setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
                mainVBox = new VBox();
                mainVBox.setFillWidth(true);
                HBox.setHgrow(mainVBox, Priority.ALWAYS);
                cellHBox = new HBox(15);
                cellHBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
                cellHBox.setAlignment(Pos.CENTER);
            }


            private void hideReactionsBox() {
                reactionsHBox.setVisible(false);
            }

            @Override
            public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    cleanup();
                    return;
                }

                subscriptions.clear();
                ChatMessage chatMessage = item.getChatMessage();

                Node flow = this.getListView().lookup(".virtual-flow");
                if (flow != null && !flow.isVisible()) {
                    return;
                }

                boolean hasTradeChatOffer = model.hasTradeChatOffer(chatMessage);
                boolean isBisqEasyPublicChatMessageWithOffer = chatMessage instanceof BisqEasyOfferbookMessage && hasTradeChatOffer;
                boolean isMyMessage = model.isMyMessage(chatMessage);

                if (isBisqEasyPublicChatMessageWithOffer) {
                    supportedLanguages.setText(controller.getSupportedLanguageCodes(((BisqEasyOfferbookMessage) chatMessage)));
                    supportedLanguages.setTooltip(new BisqTooltip(controller.getSupportedLanguageCodesForTooltip(((BisqEasyOfferbookMessage) chatMessage))));
                }

                dateTime.setVisible(false);

                cellHBox.getChildren().setAll(mainVBox);

                message.maxWidthProperty().unbind();
                if (hasTradeChatOffer) {
                    messageBgHBox.setPadding(new Insets(15));
                } else {
                    messageBgHBox.setPadding(new Insets(5, 15, 5, 15));
                }
                messageBgHBox.getStyleClass().removeAll("chat-message-bg-my-message", "chat-message-bg-peer-message");
                VBox userProfileIconVbox = new VBox(userProfileIcon);
                if (isMyMessage) {
                    buildMyMessage(isBisqEasyPublicChatMessageWithOffer, userProfileIconVbox, chatMessage);
                } else {
                    buildPeerMessage(item, isBisqEasyPublicChatMessageWithOffer, userProfileIconVbox, chatMessage);
                }

                handleQuoteMessageBox(item);
                handleReactionsBox(item);
                handleEditBox(chatMessage);

                message.setText(item.getMessage());
                dateTime.setText(item.getDate());

                item.getSenderUserProfile().ifPresent(author -> {
                    userName.setText(author.getUserName());
                    userName.setOnMouseClicked(e -> controller.onMention(author));

                    userProfileIcon.setUserProfile(author);
                    userProfileIcon.setCursor(Cursor.HAND);
                    Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
                    userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(chatMessage));
                });

                subscriptions.add(EasyBind.subscribe(item.getMessageDeliveryStatusIcon(), icon -> {
                            deliveryState.setManaged(icon != null);
                            deliveryState.setVisible(icon != null);
                            if (icon != null) {
                                AwesomeDude.setIcon(deliveryState, icon, AwesomeDude.DEFAULT_ICON_SIZE);
                                item.getMessageDeliveryStatusIconColor().ifPresent(color ->
                                        Icons.setAwesomeIconColor(deliveryState, color));
                            }
                        }
                ));
                deliveryState.getTooltip().textProperty().bind(item.getMessageDeliveryStatusTooltip());
                editInputField.maxWidthProperty().bind(message.widthProperty());

                subscriptions.add(EasyBind.subscribe(mainVBox.widthProperty(), width -> {
                    if (width == null) {
                        return;
                    }
                    mainVBox.getStyleClass().clear();

                    // List cell has no padding, so it must have the same width as list view (no scrollbar)
                    if (width.doubleValue() == list.widthProperty().doubleValue()) {
                        mainVBox.getStyleClass().add("chat-message-list-cell-wo-scrollbar");
                        return;
                    }

                    if (width.doubleValue() < CHAT_BOX_MAX_WIDTH) {
                        // List cell has different size as list view, therefore there's a scrollbar
                        mainVBox.getStyleClass().add("chat-message-list-cell-w-scrollbar-full-width");
                    } else {
                        // FIXME (low prio): needs to take into account whether there's scrollbar
                        mainVBox.getStyleClass().add("chat-message-list-cell-w-scrollbar-max-width");
                    }
                }));

                setGraphic(cellHBox);
                setAlignment(Pos.CENTER);
            }

            private void buildPeerMessage(ChatMessageListItem<? extends ChatMessage> item, boolean isBisqEasyPublicChatMessageWithOffer, VBox userProfileIconVbox, ChatMessage chatMessage) {
                // Peer
                HBox userNameAndDateHBox = new HBox(10, userName, dateTime);
                message.setAlignment(Pos.CENTER_LEFT);
                userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);

                userProfileIcon.setSize(60);
                HBox.setMargin(replyIcon, new Insets(4, 0, -4, 10));
                HBox.setMargin(pmIcon, new Insets(4, 0, -4, 0));
                HBox.setMargin(moreOptionsIcon, new Insets(6, 0, -6, 0));


                quotedMessageVBox.setId("chat-message-quote-box-peer-msg");

                messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
                if (isBisqEasyPublicChatMessageWithOffer) {
                    reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, supportedLanguages, Spacer.fillHBox());
                    message.maxWidthProperty().bind(list.widthProperty().subtract(430));
                    userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

                    Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
                    reputationLabel.getStyleClass().add("bisq-text-7");

                    reputationScoreDisplay.setReputationScore(item.getReputationScore());
                    VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
                    reputationVBox.setAlignment(Pos.CENTER_LEFT);

                    BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                    takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage, item.isCanTakeOffer()));
                    takeOfferButton.setDefaultButton(item.isCanTakeOffer());
                    takeOfferButton.setMinWidth(Control.USE_PREF_SIZE);

                    VBox messageVBox = new VBox(quotedMessageVBox, message);
                    HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                    HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                    HBox.setMargin(reputationVBox, new Insets(-5, 10, 0, 0));
                    HBox.setMargin(takeOfferButton, new Insets(0, 10, 0, 0));
                    messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox, Spacer.fillHBox(), reputationVBox, takeOfferButton);

                    VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
                    mainVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
                } else {
                    reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, Spacer.fillHBox());
                    message.maxWidthProperty().bind(list.widthProperty().subtract(140));//165
                    userProfileIcon.setSize(30);
                    userProfileIconVbox.setAlignment(Pos.TOP_LEFT);

                    VBox messageVBox = new VBox(quotedMessageVBox, message);
                    HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                    HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                    messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
                    messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());

                    VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));
                    mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, reactionsHBox);
                }
            }

            private void buildMyMessage(boolean isBisqEasyPublicChatMessageWithOffer, VBox userProfileIconVbox, ChatMessage chatMessage) {
                HBox userNameAndDateHBox = new HBox(10, dateTime, userName);
                userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
                message.setAlignment(Pos.CENTER_RIGHT);

                quotedMessageVBox.setId("chat-message-quote-box-my-msg");

                messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
                VBox.setMargin(userNameAndDateHBox, new Insets(-5, 10, -5, 0));

                VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
                if (isBisqEasyPublicChatMessageWithOffer) {
                    message.maxWidthProperty().bind(list.widthProperty().subtract(160));
                    userProfileIcon.setSize(60);
                    userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
                    HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                    HBox.setMargin(editInputField, new Insets(-4, -10, -15, 0));
                    HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

                    removeOfferButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                    reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, supportedLanguages, copyIcon);
                    reactionsHBox.setAlignment(Pos.CENTER_RIGHT);

                    HBox.setMargin(userProfileIconVbox, new Insets(0, 0, 10, 0));
                    HBox hBox = new HBox(15, messageVBox, userProfileIconVbox);
                    HBox removeOfferButtonHBox = new HBox(Spacer.fillHBox(), removeOfferButton);
                    VBox vBox = new VBox(hBox, removeOfferButtonHBox);
                    messageBgHBox.getChildren().setAll(vBox);
                } else {
                    message.maxWidthProperty().bind(list.widthProperty().subtract(140));
                    userProfileIcon.setSize(30);
                    userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
                    HBox.setMargin(deleteIcon, new Insets(0, 10, 0, 0));
                    reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, copyIcon, deleteIcon);
                    HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
                    HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                    HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
                    messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);
                }

                HBox.setMargin(deliveryState, new Insets(0, 10, 0, 0));
                HBox deliveryStateHBox = new HBox(Spacer.fillHBox(), reactionsHBox);

                subscriptions.add(EasyBind.subscribe(reactionsHBox.visibleProperty(), v -> {
                    if (v) {
                        deliveryStateHBox.getChildren().remove(deliveryState);
                        if (!reactionsHBox.getChildren().contains(deliveryState)) {
                            reactionsHBox.getChildren().add(deliveryState);
                        }
                    } else {
                        reactionsHBox.getChildren().remove(deliveryState);
                        if (!deliveryStateHBox.getChildren().contains(deliveryState)) {
                            deliveryStateHBox.getChildren().add(deliveryState);
                        }
                    }
                }));

                VBox.setMargin(deliveryStateHBox, new Insets(4, 0, -3, 0));
                mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, deliveryStateHBox);

                messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);
            }


            private void cleanup() {
                message.maxWidthProperty().unbind();
                editInputField.maxWidthProperty().unbind();

                editInputField.maxWidthProperty().unbind();
                removeOfferButton.setOnAction(null);
                takeOfferButton.setOnAction(null);

                saveEditButton.setOnAction(null);
                cancelEditButton.setOnAction(null);

                userName.setOnMouseClicked(null);
                userProfileIcon.setOnMouseClicked(null);
                replyIcon.setOnMouseClicked(null);
                pmIcon.setOnMouseClicked(null);
                editIcon.setOnMouseClicked(null);
                copyIcon.setOnMouseClicked(null);
                deleteIcon.setOnMouseClicked(null);
                moreOptionsIcon.setOnMouseClicked(null);

                editInputField.setOnKeyPressed(null);

                cellHBox.setOnMouseEntered(null);
                cellHBox.setOnMouseExited(null);

                userProfileIcon.releaseResources();

                subscriptions.forEach(Subscription::unsubscribe);
                subscriptions.clear();

                setGraphic(null);
            }

            private void handleEditBox(ChatMessage chatMessage) {
                saveEditButton.setOnAction(e -> {
                    controller.onSaveEditedMessage(chatMessage, editInputField.getText());
                    onCloseEditMessage();
                });
                cancelEditButton.setOnAction(e -> onCloseEditMessage());
            }

            private void handleReactionsBox(ChatMessageListItem<? extends ChatMessage> item) {
                ChatMessage chatMessage = item.getChatMessage();
                boolean isMyMessage = model.isMyMessage(chatMessage);

                boolean isPublicChannel = model.getIsPublicChannel().get();
                boolean allowEditing = isPublicChannel;
                if (chatMessage instanceof BisqEasyOfferbookMessage) {
                    BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                    allowEditing = allowEditing && bisqEasyOfferbookMessage.getBisqEasyOffer().isEmpty();
                }
                if (isMyMessage) {
                    copyIcon.setOnMouseClicked(e -> controller.onCopyMessage(chatMessage));
                    if (allowEditing) {
                        editIcon.setOnMouseClicked(e -> onEditMessage(item));
                    }
                    if (isPublicChannel) {
                        deleteIcon.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
                    }
                } else {
                    moreOptionsIcon.setOnMouseClicked(e -> controller.onOpenMoreOptions(pmIcon, chatMessage, () -> {
                        hideReactionsBox();
                        model.getSelectedChatMessageForMoreOptionsPopup().set(null);
                    }));
                    replyIcon.setOnMouseClicked(e -> controller.onReply(chatMessage));
                    pmIcon.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
                }

                replyIcon.setVisible(!isMyMessage);
                replyIcon.setManaged(!isMyMessage);

                pmIcon.setVisible(!isMyMessage && chatMessage instanceof PublicChatMessage);
                pmIcon.setManaged(!isMyMessage && chatMessage instanceof PublicChatMessage);

                editIcon.setVisible(isMyMessage && allowEditing);
                editIcon.setManaged(isMyMessage && allowEditing);
                deleteIcon.setVisible(isMyMessage && isPublicChannel);
                deleteIcon.setManaged(isMyMessage && isPublicChannel);
                removeOfferButton.setVisible(isMyMessage && isPublicChannel);
                removeOfferButton.setManaged(isMyMessage && isPublicChannel);

                setOnMouseEntered(e -> {
                    if (model.getSelectedChatMessageForMoreOptionsPopup().get() != null || editInputField.isVisible()) {
                        return;
                    }
                    dateTime.setVisible(true);
                    reactionsHBox.setVisible(true);
                });

                setOnMouseExited(e -> {
                    if (model.getSelectedChatMessageForMoreOptionsPopup().get() == null) {
                        hideReactionsBox();
                        dateTime.setVisible(false);
                        reactionsHBox.setVisible(false);
                    }
                });
            }

            private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage> item) {
                Optional<Citation> optionalCitation = item.getCitation();
                if (optionalCitation.isPresent()) {
                    Citation citation = optionalCitation.get();
                    if (citation.isValid()) {
                        quotedMessageVBox.setVisible(true);
                        quotedMessageVBox.setManaged(true);
                        quotedMessageField.setText(citation.getText());
                        quotedMessageField.setStyle("-fx-fill: -fx-mid-text-color");
                        Label userName = new Label(controller.getUserName(citation.getAuthorUserProfileId()));
                        userName.getStyleClass().add("font-medium");
                        userName.setStyle("-fx-text-fill: -bisq-mid-grey-30");
                        quotedMessageVBox.getChildren().setAll(userName, quotedMessageField);
                    }
                } else {
                    quotedMessageVBox.getChildren().clear();
                    quotedMessageVBox.setVisible(false);
                    quotedMessageVBox.setManaged(false);
                }
            }

            private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                reactionsHBox.setVisible(false);
                editInputField.setVisible(true);
                editInputField.setManaged(true);
                editInputField.setInitialHeight(message.getBoundsInLocal().getHeight());
                editInputField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                editInputField.requestFocus();
                editInputField.positionCaret(message.getText().length());
                editButtonsHBox.setVisible(true);
                editButtonsHBox.setManaged(true);
                removeOfferButton.setVisible(false);
                removeOfferButton.setManaged(false);

                message.setVisible(false);
                message.setManaged(false);

                editInputField.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        event.consume();
                        if (event.isShiftDown()) {
                            editInputField.appendText(System.getProperty("line.separator"));
                        } else if (!editInputField.getText().isEmpty()) {
                            controller.onSaveEditedMessage(item.getChatMessage(), editInputField.getText().trim());
                            onCloseEditMessage();
                        }
                    }
                });
            }

            private void onCloseEditMessage() {
                editInputField.setVisible(false);
                editInputField.setManaged(false);
                editButtonsHBox.setVisible(false);
                editButtonsHBox.setManaged(false);
                removeOfferButton.setVisible(true);
                removeOfferButton.setManaged(true);

                message.setVisible(true);
                message.setManaged(true);
                editInputField.setOnKeyPressed(null);
            }
        };
    }

    private Label getIconWithToolTip(AwesomeIcon icon, String tooltipString) {
        Label iconLabel = Icons.getIcon(icon);
        iconLabel.setCursor(Cursor.HAND);
        iconLabel.setTooltip(new BisqTooltip(tooltipString, true));
        return iconLabel;
    }
}
