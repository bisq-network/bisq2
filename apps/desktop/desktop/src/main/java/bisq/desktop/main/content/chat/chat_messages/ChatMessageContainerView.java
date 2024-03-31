package bisq.desktop.main.content.chat.chat_messages;

import bisq.chat.ChatChannel;
import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.chat.ChatUtil;
import bisq.desktop.main.content.components.ChatMentionPopupMenu;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class ChatMessageContainerView extends bisq.desktop.common.view.View<VBox, ChatMessageContainerModel, ChatMessageContainerController> {
    private final static double CHAT_BOX_MAX_WIDTH = 1200;
    public final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");
    @Getter
    private final BisqTextArea inputField = new BisqTextArea(); //todo remove accessor
    private final Button sendButton = new Button();
    private final Pane messagesListView;
    private final VBox emptyMessageList;
    private ChatMentionPopupMenu<UserProfile> userMentionPopup;
    private ChatMentionPopupMenu<ChatChannel<?>> channelMentionPopup;
    private Pane userProfileSelectionRoot;
    private Subscription focusInputTextFieldPin;

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

        userMentionPopup.setItems(model.getMentionableUsers());
        channelMentionPopup.setItems(model.getMentionableChatChannels());

        createChatDialogEnabledSubscription();

        focusInputTextFieldPin = EasyBind.subscribe(model.getFocusInputTextField(), focusInputTextField -> {
            if (focusInputTextField != null && focusInputTextField) {
                inputField.requestFocus();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        userProfileSelectionRoot.visibleProperty().unbind();
        userProfileSelectionRoot.managedProperty().unbind();
        inputField.textProperty().unbindBidirectional(model.getTextInput());
        userMentionPopup.filterProperty().unbind();
        channelMentionPopup.filterProperty().unbind();
        focusInputTextFieldPin.unsubscribe();
        removeChatDialogEnabledSubscription();

        inputField.setOnKeyPressed(null);
        sendButton.setOnAction(null);
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
        sendButton.setTooltip(new BisqTooltip(Res.get("chat.message.input.send"), true));

        HBox sendMessageBox = new HBox(inputField, sendButton);
        sendMessageBox.getStyleClass().add("chat-send-message-box");
        sendMessageBox.setAlignment(Pos.BOTTOM_CENTER);
        HBox.setHgrow(sendMessageBox, Priority.ALWAYS);
        return sendMessageBox;
    }

    private void setUpInputFieldAtMentions() {
        userMentionPopup = new ChatMentionPopupMenu<>(inputField);
        userMentionPopup.setItemDisplayConverter(UserProfile::getUserName);
        userMentionPopup.setSelectionHandler(controller::onListUserNames);

        channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
        channelMentionPopup.setItemDisplayConverter(model::getChannelTitle);
        channelMentionPopup.setSelectionHandler(controller::onListChannels);
    }

    private void setUpUserProfileSelection(UserProfileSelection userProfileSelection) {
        userProfileSelection.setMaxComboBoxWidth(165);
        userProfileSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserProfileSelection.ListItem item) {
                return item != null ? StringUtils.truncate(item.getUserIdentity().getUserName(), 10) : "";
            }

            @Override
            public UserProfileSelection.ListItem fromString(String string) {
                return null;
            }
        });
        userProfileSelectionRoot = userProfileSelection.getRoot();
        userProfileSelectionRoot.setMaxHeight(44);
        userProfileSelectionRoot.setMaxWidth(165);
        userProfileSelectionRoot.setMinWidth(165);
        userProfileSelectionRoot.setId("chat-user-profile-bg");
        HBox.setMargin(userProfileSelectionRoot, new Insets(0, -20, 0, -8));
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
}
