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

package bisq.desktop.main.content.components.chatMessages.messages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.desktop.common.Icons;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

public final class MyMessage extends Message {
    private final static double CHAT_MESSAGE_BOX_MAX_WIDTH = 630;
    private final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");

    private final ChatMessagesListView.Controller controller;
    private final ChatMessagesListView.Model model;
    private final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
    private final Label message, userName, dateTime, editIcon, deleteIcon, copyIcon;
    private final Label quotedMessageField = new Label();
    private final VBox quotedMessageVBox;
    private final HBox messageHBox, messageBgHBox;
    private final Button removeOfferButton;
    private final Label deliveryState;
    private final BisqTextArea editInputField;
    private final Button saveEditButton, cancelEditButton;
    private final HBox reactionsHBox, editButtonsHBox;
    private Subscription reactionsVisiblePropertyPin, messageDeliveryStatusIconPin;

    public MyMessage(final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                     ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                     ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        this.controller = controller;
        this.model = model;

        // userName and DateTime
        userName = new Label();
        userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");
        dateTime = new Label();
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");
        dateTime.setVisible(false);
        dateTime.setText(item.getDate());

        HBox userNameAndDateHBox = new HBox(10, dateTime, userName);
        userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 10, -5, 0));

        // userProfileIcon
        userProfileIcon.setSize(60);
        VBox userProfileIconVbox = new VBox(userProfileIcon);

        item.getSenderUserProfile().ifPresent(author -> {
            userName.setText(author.getUserName());
            userName.setOnMouseClicked(e -> controller.onMention(author));

            userProfileIcon.setUserProfile(author);
            userProfileIcon.setCursor(Cursor.HAND);
            Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
            userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
        });

        // removeOfferButton
        removeOfferButton = new Button(Res.get("offer.deleteOffer"));
        removeOfferButton.getStyleClass().addAll("red-small-button", "no-background");

        // reactions
        editIcon = getIconWithToolTip(AwesomeIcon.EDIT, Res.get("action.edit"));
        copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
        deleteIcon = getIconWithToolTip(AwesomeIcon.REMOVE_SIGN, Res.get("action.delete"));
        HBox.setMargin(editIcon, new Insets(1, 0, 0, 0));
        reactionsHBox = new HBox(20);
        reactionsHBox.setVisible(false);
        handleReactionsBox(item);

        // supportedLanguages
        Label supportedLanguages = new Label();
        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            supportedLanguages.setText(item.getSupportedLanguageCodes(((BisqEasyOfferbookMessage) item.getChatMessage())));
            supportedLanguages.setTooltip(new BisqTooltip(item.getSupportedLanguageCodesForTooltip(((BisqEasyOfferbookMessage) item.getChatMessage()))));
        }

        // edit
        editInputField = new BisqTextArea();
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
        VBox.setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
        handleEditBox(item.getChatMessage());

        // quoted message
        quotedMessageVBox = new VBox(5);
        quotedMessageVBox.setVisible(false);
        quotedMessageVBox.setManaged(false);
        quotedMessageVBox.setId("chat-message-quote-box-my-msg");
        VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
        quotedMessageField.setWrapText(true);
        handleQuoteMessageBox(item);

        // HBox for message reputation vBox and action button
        message = new Label();
        message.maxWidthProperty().unbind();
        message.setWrapText(true);
        message.setPadding(new Insets(10));
        message.getStyleClass().addAll("text-fill-white", "normal-text", "font-default");
        message.setAlignment(Pos.CENTER_RIGHT);
        message.setText(item.getMessage());

        // message background
        messageBgHBox = new HBox(15);
        messageBgHBox.setAlignment(Pos.CENTER_LEFT);
        messageBgHBox.setMaxWidth(CHAT_MESSAGE_BOX_MAX_WIDTH);
        messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
        HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
        if (item.hasTradeChatOffer()) {
            messageBgHBox.setPadding(new Insets(15));
        } else {
            messageBgHBox.setPadding(new Insets(5, 15, 5, 15));
        }

        // messageHBox
        messageHBox = new HBox();
        VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));

        // deliveryState
        deliveryState = new Label();
        deliveryState.setCursor(Cursor.HAND);
        deliveryState.setTooltip(new BisqTooltip(true));

        VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
        // TODO (refactor): Move this logic to BisqEasy package
        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            message.maxWidthProperty().bind(list.widthProperty().subtract(160));
            userProfileIcon.setSize(60);
            userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
            HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
            HBox.setMargin(editInputField, new Insets(-4, -10, -15, 0));
            HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

            removeOfferButton.setOnAction(e -> controller.onDeleteMessage(item.getChatMessage()));
            reactionsHBox.getChildren().setAll(Spacer.fillHBox(), editIcon, supportedLanguages, copyIcon);
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
            reactionsHBox.getChildren().setAll(Spacer.fillHBox(), editIcon, copyIcon, deleteIcon);
            HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
            HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
            HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
            messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);
        }

        HBox.setMargin(deliveryState, new Insets(0, 10, 0, 0));
        HBox deliveryStateHBox = new HBox(Spacer.fillHBox(), reactionsHBox);

        reactionsVisiblePropertyPin = EasyBind.subscribe(reactionsHBox.visibleProperty(), v -> {
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
        });

        messageDeliveryStatusIconPin = EasyBind.subscribe(item.getMessageDeliveryStatusIcon(), icon -> {
                deliveryState.setManaged(icon != null);
                deliveryState.setVisible(icon != null);
                if (icon != null) {
                    AwesomeDude.setIcon(deliveryState, icon, AwesomeDude.DEFAULT_ICON_SIZE);
                    item.getMessageDeliveryStatusIconColor().ifPresent(color ->
                            Icons.setAwesomeIconColor(deliveryState, color));
                }
            }
        );

        deliveryState.getTooltip().textProperty().bind(item.getMessageDeliveryStatusTooltip());
        editInputField.maxWidthProperty().bind(message.widthProperty());

        VBox.setMargin(deliveryStateHBox, new Insets(4, 0, -3, 0));
        messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);
        getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, deliveryStateHBox);
        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    // TODO: move outside
    private static Label getIconWithToolTip(AwesomeIcon icon, String tooltipString) {
        Label iconLabel = Icons.getIcon(icon);
        iconLabel.setCursor(Cursor.HAND);
        iconLabel.setTooltip(new BisqTooltip(tooltipString, true));
        return iconLabel;
    }

    private void handleEditBox(ChatMessage chatMessage) {
        saveEditButton.setOnAction(e -> {
            controller.onSaveEditedMessage(chatMessage, editInputField.getText());
            onCloseEditMessage();
        });
        cancelEditButton.setOnAction(e -> onCloseEditMessage());
    }

    private void handleReactionsBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        ChatMessage chatMessage = item.getChatMessage();
        boolean isPublicChannel = item.isPublicChannel();
        boolean allowEditing = isPublicChannel;
        if (chatMessage instanceof BisqEasyOfferbookMessage) {
            BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
            allowEditing = allowEditing && bisqEasyOfferbookMessage.getBisqEasyOffer().isEmpty();
        }

        // myMessage
        copyIcon.setOnMouseClicked(e -> controller.onCopyMessage(chatMessage));
        if (allowEditing) {
            editIcon.setOnMouseClicked(e -> onEditMessage(item));
        }
        if (isPublicChannel) {
            deleteIcon.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
        }

        editIcon.setVisible(allowEditing);
        editIcon.setManaged(allowEditing);
        deleteIcon.setVisible(isPublicChannel);
        deleteIcon.setManaged(isPublicChannel);
        removeOfferButton.setVisible(isPublicChannel);
        removeOfferButton.setManaged(isPublicChannel);

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

    private void hideReactionsBox() {
        reactionsHBox.setVisible(false);
    }

    private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
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

    private void onEditMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
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

    @Override
    public void cleanup() {
        message.maxWidthProperty().unbind();
        editInputField.maxWidthProperty().unbind();

        editInputField.maxWidthProperty().unbind();
        removeOfferButton.setOnAction(null);

        saveEditButton.setOnAction(null);
        cancelEditButton.setOnAction(null);

        userName.setOnMouseClicked(null);
        userProfileIcon.setOnMouseClicked(null);

        editIcon.setOnMouseClicked(null);
        copyIcon.setOnMouseClicked(null);
        deleteIcon.setOnMouseClicked(null);

        editInputField.setOnKeyPressed(null);
        userProfileIcon.releaseResources();

        if (reactionsVisiblePropertyPin != null) {
            reactionsVisiblePropertyPin.unsubscribe();
        }

        if (messageDeliveryStatusIconPin != null) {
            messageDeliveryStatusIconPin.unsubscribe();
        }
    }
}
