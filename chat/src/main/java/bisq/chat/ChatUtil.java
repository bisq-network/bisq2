package bisq.chat;

import bisq.chat.priv.PrivateChatChannel;
import bisq.i18n.Res;

public class ChatUtil {
    public static String getChannelNavigationPath(ChatChannel<?> chatChannel) {
        String channelDomain = chatChannel.getChatChannelDomain().getDisplayString();
        String channelTitle = chatChannel instanceof PrivateChatChannel
                ? Res.get("chat.notifications.privateMessage.headline")
                : chatChannel.getDisplayString();
        return channelDomain + " > " + channelTitle;
    }
}
