package bisq.desktop.main.content.components.chatMessages.messages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SystemMessage extends Message {
    protected final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item;
    protected final VBox systemMessageBg = new VBox();
    protected final VBox contentVBox;
    protected final Label message, dateTime;

    public SystemMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        this.item = item;

        message = new Label(item.getMessage());
        message.getStyleClass().addAll("text-fill-white", "system-message-labels");
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);

        dateTime = new Label(item.getDate());
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "system-message-labels");

        systemMessageBg.setSpacing(5);
        systemMessageBg.getChildren().addAll(message, dateTime);
        systemMessageBg.setFillWidth(true);
        systemMessageBg.setAlignment(Pos.CENTER);
        systemMessageBg.getStyleClass().add("system-message-background");
        HBox.setHgrow(systemMessageBg, Priority.ALWAYS);

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));

        contentVBox = new VBox(systemMessageBg);
        contentVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        getChildren().setAll(contentVBox);
        setAlignment(Pos.CENTER);
    }

    @Override
    public void cleanup() {
    }
}
