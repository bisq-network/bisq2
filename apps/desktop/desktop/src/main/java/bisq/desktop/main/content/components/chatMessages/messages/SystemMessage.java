package bisq.desktop.main.content.components.chatMessages.messages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import javafx.scene.control.Label;

public class SystemMessage extends Message {
    protected final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item;

    public SystemMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        this.item = item;

        Label label = new Label(item.getMessage());
        getChildren().setAll(label);
    }

    @Override
    public void cleanup() {
    }
}
