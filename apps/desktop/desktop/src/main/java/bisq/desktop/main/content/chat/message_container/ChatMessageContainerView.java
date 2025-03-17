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

package bisq.desktop.main.content.chat.message_container;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.desktop.main.content.chat.message_container.components.ChatMentionPopupMenu;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.stream.Collectors;

import static javafx.scene.input.KeyEvent.KEY_PRESSED;

@Slf4j
public class ChatMessageContainerView extends bisq.desktop.common.view.View<VBox, ChatMessageContainerModel, ChatMessageContainerController> {
    private final static double CHAT_BOX_MAX_WIDTH = 1200;
    public final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");
    private final BisqTextArea inputField = new BisqTextArea();
    private final EventHandler<KeyEvent> enterKeyPressedHandler = this::processEnterKeyPressed;
    private final Button sendButton = new Button();
    private final Pane messagesListView;
    private final VBox emptyMessageList;
    private ChatMentionPopupMenu userMentionPopup;
    private Pane userProfileSelectionRoot;
    private Subscription focusInputTextFieldPin, caretPositionPin;

    public ChatMessageContainerView(ChatMessageContainerModel model,
                                    ChatMessageContainerController controller,
                                    Pane messagesListView,
                                    Pane quotedMessageBlock,
                                    UserProfileSelection userProfileSelection) {
        super(new VBox(), model, controller);

        this.messagesListView = messagesListView;
        VBox.setVgrow(messagesListView, Priority.ALWAYS);

        emptyMessageList = ChatUtil.createEmptyChatPlaceholder(
                new Label(Res.get("chat.private.messagebox.noChats.placeholder.title")),
                new Label(Res.get("chat.private.messagebox.noChats.placeholder.description")));

        VBox bottomBarContainer = createAndGetBottomBar(userProfileSelection);

        quotedMessageBlock.setMaxWidth(CHAT_BOX_MAX_WIDTH);

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(messagesListView, emptyMessageList, quotedMessageBlock, bottomBarContainer);
    }

    @Override
    protected void onViewAttached() {
        userProfileSelectionRoot.visibleProperty().bind(model.getUserProfileSelectionVisible());
        userProfileSelectionRoot.managedProperty().bind(model.getUserProfileSelectionVisible());
        inputField.textProperty().bindBidirectional(model.getTextInput());

        caretPositionPin = EasyBind.subscribe(model.getCaretPosition(), position -> {
            if (position != null) {
                inputField.positionCaret(position.intValue());
            }
        });

        inputField.addEventFilter(KEY_PRESSED, enterKeyPressedHandler);

        sendButton.setOnAction(event -> {
            controller.onSendMessage(inputField.getText().trim());
            inputField.clear();
        });

        userMentionPopup.getObservableList().setAll(model.getMentionableUsers().stream()
                .map(ChatMentionPopupMenu.ListItem::new)
                .collect(Collectors.toList()));

        createChatDialogEnabledSubscription();

        focusInputTextFieldPin = EasyBind.subscribe(model.getFocusInputTextField(), focusInputTextField -> {
            if (focusInputTextField != null && focusInputTextField) {
                inputField.requestFocus();
            }
        });
        userMentionPopup.init();

        UIThread.runOnNextRenderFrame(inputField::requestFocus);
    }

    @Override
    protected void onViewDetached() {
        userProfileSelectionRoot.visibleProperty().unbind();
        userProfileSelectionRoot.managedProperty().unbind();
        inputField.textProperty().unbindBidirectional(model.getTextInput());
        focusInputTextFieldPin.unsubscribe();
        caretPositionPin.unsubscribe();
        removeChatDialogEnabledSubscription();

        inputField.setOnKeyPressed(null);
        inputField.removeEventFilter(KEY_PRESSED, enterKeyPressedHandler);
        sendButton.setOnAction(null);
        userMentionPopup.cleanup();
    }

    private VBox createAndGetBottomBar(UserProfileSelection userProfileSelection) {
        setUpUserProfileSelection(userProfileSelection);
        HBox sendMessageBox = createAndGetSendMessageBox();

        HBox bottomBar = new HBox(10);
        bottomBar.getChildren().addAll(userProfileSelectionRoot, sendMessageBox);
        bottomBar.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        bottomBar.setPadding(new Insets(14, 20, 14, 20));
        bottomBar.setAlignment(Pos.BOTTOM_CENTER);

        VBox bottomBarContainer = new VBox(bottomBar);
        bottomBarContainer.setAlignment(Pos.CENTER);
        return bottomBarContainer;
    }

    private HBox createAndGetSendMessageBox() {
        inputField.setPromptText(Res.get("chat.message.input.prompt"));
        inputField.getStyleClass().addAll("chat-input-field", "medium-text");
        inputField.setPadding(new Insets(5, 0, 5, 5));
        HBox.setMargin(inputField, new Insets(0, 0, 1.5, 0));
        HBox.setHgrow(inputField, Priority.ALWAYS);
        setUpInputFieldAtMentions();

        sendButton.setGraphic(ImageUtil.getImageViewById("chat-send"));
        sendButton.setId("chat-messages-send-button");
        HBox.setMargin(sendButton, new Insets(0, 0, 5, 0));
        sendButton.setMinWidth(30);
        sendButton.setMaxWidth(30);
        sendButton.setTooltip(new BisqTooltip(Res.get("chat.message.input.send"), BisqTooltip.Style.DARK));

        HBox sendMessageBox = new HBox(inputField, sendButton);
        sendMessageBox.getStyleClass().add("chat-send-message-box");
        sendMessageBox.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(sendMessageBox, Priority.ALWAYS);
        return sendMessageBox;
    }

    private void setUpInputFieldAtMentions() {
        userMentionPopup = new ChatMentionPopupMenu(inputField, controller::onUserProfileSelected);
    }

    private void setUpUserProfileSelection(UserProfileSelection userProfileSelection) {
        userProfileSelection.setMaxWidth(165);
        userProfileSelection.openMenuUpwards();
        userProfileSelection.openMenuToTheRight();
        userProfileSelectionRoot = userProfileSelection.getRoot();
        userProfileSelectionRoot.setMaxHeight(45);
        userProfileSelectionRoot.setMaxWidth(165);
        userProfileSelectionRoot.setMinWidth(165);
        userProfileSelectionRoot.getStyleClass().add("chat-user-profile-bg");
    }

    private void createChatDialogEnabledSubscription() {
        inputField.disableProperty().bind(model.getChatDialogEnabled().not());
        sendButton.disableProperty().bind(model.getChatDialogEnabled().not());
        emptyMessageList.visibleProperty().bind(model.getChatDialogEnabled().not());
        emptyMessageList.managedProperty().bind(model.getChatDialogEnabled().not());
        messagesListView.visibleProperty().bind(model.getChatDialogEnabled());
        messagesListView.managedProperty().bind(model.getChatDialogEnabled());
        userProfileSelectionRoot.disableProperty().bind(model.getChatDialogEnabled().not());
    }

    private void removeChatDialogEnabledSubscription() {
        inputField.disableProperty().unbind();
        sendButton.disableProperty().unbind();
        emptyMessageList.visibleProperty().unbind();
        emptyMessageList.managedProperty().unbind();
        messagesListView.visibleProperty().unbind();
        messagesListView.managedProperty().unbind();
        userProfileSelectionRoot.disableProperty().unbind();
    }

    private void processEnterKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            keyEvent.consume();
            if (keyEvent.isShiftDown()) {
                int caretPosition = inputField.getCaretPosition();
                inputField.insertText(caretPosition, System.lineSeparator());
                inputField.positionCaret(caretPosition + System.lineSeparator().length());
            } else if (!inputField.getText().isEmpty()) {
                controller.onSendMessage(inputField.getText().trim());
                inputField.clear();
            }
        }
    }
}
