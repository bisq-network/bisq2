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

    public SystemMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        this.item = item;

        Label message = new Label(item.getMessage());
        message.getStyleClass().addAll("text-fill-white", "system-message-labels");
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);

        Label dateTime = new Label(item.getDate());
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "system-message-labels");

        VBox vBox = new VBox(5, message, dateTime);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.CENTER);
        vBox.getStyleClass().add("system-message-background");
        HBox.setHgrow(vBox, Priority.ALWAYS);

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));
        getChildren().setAll(vBox);
    }

    @Override
    public void cleanup() {
    }
}
