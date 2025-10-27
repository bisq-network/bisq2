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

package bisq.desktop.main.content.chat.message_container.list.message_box;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public final class MyTextMessageBox extends BubbleMessageBox {
    private final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");
    private MessageDeliveryStatusBox messageDeliveryStatusBox;

    private final Subscription setAsEditingPin;
    private BisqMenuItem editAction, deleteAction;
    private BisqTextArea editInputField;
    private Button saveEditButton, cancelEditButton;
    private HBox editButtonsHBox;

    private final javafx.event.EventHandler<KeyEvent> editInputFieldKeyPressedFilter;

    public MyTextMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                            ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                            ChatMessagesListController controller) {
        super(item, list, controller);

        quotedMessageVBox.setId("chat-message-quote-box-my-msg");
        setUpEditFunctionality();
        message.setAlignment(Pos.CENTER_RIGHT);
        messageBgHBox.getStyleClass().add("chat-message-bg-my-message");

        VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);

        message.maxWidthProperty().bind(list.widthProperty().subtract(140));
        userProfileIcon.setSize(30);
        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
        actionsHBox.getChildren().setAll(Spacer.fillHBox(), reactMenuBox, editAction, copy, deleteAction);
        HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
        HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
        messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);

        activeReactionsDisplayHBox.getStyleClass().add("my-text-message-box-active-reactions");
        editInputField.maxWidthProperty().bind(message.widthProperty());
        messageHBox.getChildren().setAll(Spacer.fillHBox(), activeReactionsDisplayHBox, messageBgHBox);
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, actionsHBox);

        editInputFieldKeyPressedFilter = createEditInputFieldKeyPressedFilter(item, controller);

        setAsEditingPin = EasyBind.subscribe(item.getSetAsEditing(), setAsEditing -> {
            if (setAsEditing) {
                UIThread.runOnNextRenderFrame(() -> {
                    onEditMessage();
                    item.getSetAsEditing().set(false);
                });
            }
        });
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        messageDeliveryStatusBox = new MessageDeliveryStatusBox(item, controller);

        userNameAndDateHBox = new HBox(10, dateTime, messageDeliveryStatusBox, item.getBondedRoleBadge(), userName);
        userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
        setMargin(userNameAndDateHBox, new Insets(-5, 10, -5, 0));
    }

    @Override
    protected void setUpActions() {
        super.setUpActions();

        reactMenuBox.setSlideToTheLeft();
        reactMenuBox.reverseReactionsDisplayOrder();
        editAction = new BisqMenuItem("edit-grey", "edit-white");
        editAction.useIconOnly();
        editAction.setTooltip(Res.get("action.edit"));
        deleteAction = new BisqMenuItem("delete-t-grey", "delete-t-red");
        deleteAction.useIconOnly();
        deleteAction.setTooltip(Res.get("action.delete"));
        HBox.setMargin(editAction, ACTION_ITEMS_MARGIN);
        HBox.setMargin(deleteAction, ACTION_ITEMS_MARGIN);
    }

    private EventHandler<KeyEvent> createEditInputFieldKeyPressedFilter(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                                                        ChatMessagesListController controller) {
        return keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                keyEvent.consume(); // Good practice
                if (keyEvent.isShiftDown()) {
                    int caretPos = editInputField.getCaretPosition();
                    String currentText = editInputField.getText();
                    String newText = currentText.substring(0, caretPos) + System.lineSeparator() + currentText.substring(caretPos);
                    editInputField.setText(newText);
                    editInputField.positionCaret(caretPos + 1);
                } else if (!editInputField.getText().trim().isEmpty()) { // trim() here is good
                    controller.onSaveEditedMessageUsingEnterKeyShortcut(item.getChatMessage(), editInputField.getText().trim());
                    onCloseEditMessage(); // This will remove the filter
                }
            } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
                keyEvent.consume();
                onCloseEditMessage();
            } else if (keyEvent.getCode() == KeyCode.UP) {
                String normalizedText = StringUtils.normalizeLineBreaks(editInputField.getText());
                // If no line break is found from the start to the caret position, it means we are in the first line, so we should move to the start
                if (normalizedText.indexOf(System.lineSeparator(), 0, editInputField.getCaretPosition()) == -1) {
                    // Only consume event in this case, otherwise allow falling back to default behavior
                    keyEvent.consume();
                    editInputField.positionCaret(0);
                }
            }
        };
    }

    private void setUpEditFunctionality() {
        // edit
        editInputField = new BisqTextArea();
        editInputField.getStyleClass().addAll("text-fill-white", "normal-text", "font-default");
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
        setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
        handleEditBox();
    }

    @Override
    protected void addActionsHandlers() {
        ChatMessage chatMessage = item.getChatMessage();
        boolean isPublicChannel = item.isPublicChannel();
        boolean allowEditing = isPublicChannel;
        if (chatMessage instanceof BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
            allowEditing = allowEditing && bisqEasyOfferbookMessage.getBisqEasyOffer().isEmpty();
        }

        copy.setOnAction(e -> onCopyMessage(chatMessage));
        if (allowEditing) {
            editAction.setOnAction(e -> onEditMessage());
        }
        if (isPublicChannel) {
            deleteAction.setOnAction(e -> controller.onDeleteMessage(chatMessage));
        }

        editAction.setVisible(allowEditing);
        editAction.setManaged(allowEditing);
        deleteAction.setVisible(isPublicChannel);
        deleteAction.setManaged(isPublicChannel);
    }

    private void handleEditBox() {
        saveEditButton.setOnAction(e -> {
            controller.onSaveEditedMessage(item.getChatMessage(), editInputField.getText());
            onCloseEditMessage();
        });
        cancelEditButton.setOnAction(e -> onCloseEditMessage());
    }

    @Override
    protected void showActionsHBox() {
        actionsHBox.setVisible(!editButtonsHBox.isVisible());
    }

    private void onEditMessage() {
        actionsHBox.setVisible(false);
        editInputField.setVisible(true);
        editInputField.setManaged(true);
        editInputField.setInitialHeight(message.getBoundsInLocal().getHeight());
        editInputField.setText(message.getText().replace(EDITED_POST_FIX, ""));
        editInputField.requestFocus();
        editInputField.positionCaret(message.getText().length());
        editButtonsHBox.setVisible(true);
        editButtonsHBox.setManaged(true);
        message.setVisible(false);
        message.setManaged(false);
        editInputField.addEventFilter(KeyEvent.KEY_PRESSED, editInputFieldKeyPressedFilter);
    }

    private void onCloseEditMessage() {
        editInputField.setVisible(false);
        editInputField.setManaged(false);
        editButtonsHBox.setVisible(false);
        editButtonsHBox.setManaged(false);
        message.setVisible(true);
        message.setManaged(true);
        editInputField.removeEventFilter(KeyEvent.KEY_PRESSED, editInputFieldKeyPressedFilter);
    }

    @Override
    public void dispose() {
        super.dispose();

        message.maxWidthProperty().unbind();
        editInputField.maxWidthProperty().unbind();

        saveEditButton.setOnAction(null);
        cancelEditButton.setOnAction(null);
        copy.setOnAction(null);
        editAction.setOnAction(null);
        deleteAction.setOnAction(null);

        userName.setOnMouseClicked(null);
        userProfileIcon.setOnMouseClicked(null);
        userProfileIcon.dispose();

        messageDeliveryStatusBox.dispose();
        editInputField.removeEventFilter(KeyEvent.KEY_PRESSED, editInputFieldKeyPressedFilter);

        setAsEditingPin.unsubscribe();
    }
}
