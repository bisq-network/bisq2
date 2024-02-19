package bisq.desktop.main.content.components.chatMessages.messages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.i18n.Res;
import javafx.scene.control.Hyperlink;

public final class LeaveChatMessage extends SystemMessage {
    public LeaveChatMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                            ChatMessagesListView.Controller controller) {
        super(item);

        Hyperlink hyperlink = new Hyperlink(Res.get("chat.leave"));
        hyperlink.setGraphic(ImageUtil.getImageViewById("leave-chat-green"));
        hyperlink.getStyleClass().addAll("system-message-labels", "leave-chat-message");
        hyperlink.setOnAction(e -> controller.onLeaveChannel());
        systemMessageBg.getChildren().setAll(message, hyperlink, dateTime);
    }
}
