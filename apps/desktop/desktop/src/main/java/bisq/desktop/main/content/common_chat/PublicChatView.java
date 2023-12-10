package bisq.desktop.main.content.common_chat;

import javafx.scene.layout.Pane;

public class PublicChatView extends CommonChatView {
    public PublicChatView(CommonChatModel model, PublicChatController controller, Pane chatMessagesComponent, Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }
}
