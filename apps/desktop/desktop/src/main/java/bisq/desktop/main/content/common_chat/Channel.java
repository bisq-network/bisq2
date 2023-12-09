package bisq.desktop.main.content.common_chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Channel implements Comparable<Channel> {
    @EqualsAndHashCode.Include
    private final String channelId;
    private final ChatChannelDomain chatChannelDomain;
    private final ChatChannel<?> chatChannel;
    private final String channelTitle;
    private final String iconId;
    private final String iconIdHover;
    private final String iconIdSelected;
    private final NavigationTarget navigationTarget;
    @Setter
    private boolean isSelected;

    public Channel(CommonPublicChatChannel chatChannel, CommonPublicChatChannelService chatChannelService,
                   NavigationTarget navigationTarget) {
        this.chatChannel = chatChannel;
        chatChannelDomain = chatChannel.getChatChannelDomain();
        channelId = chatChannel.getId();
        channelTitle = chatChannelService.getChannelTitle(chatChannel);
        String styleToken = channelId.replace(".", "-");
        iconIdSelected = "channels-" + styleToken;
        iconIdHover = "channels-" + styleToken + "-white";
        iconId = "channels-" + styleToken + "-grey";
        this.navigationTarget = navigationTarget;
    }

    @Override
    public int compareTo(Channel other) {
        return this.channelTitle.compareToIgnoreCase(other.channelTitle);
    }
}
