package bisq.restApi.dto;

import bisq.chat.ChatService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Optional;


@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@ToString
@Schema(name = "PublicDiscussionChannel")
public final class CommonPublicChatChannelDto {
    @EqualsAndHashCode.Include
    private String id;
    private ChatChannelDomain chatChannelDomain;
    private String channelTitle;
    private String description;
    private Optional<String> channelAdminId;
    private List<String> channelModeratorIds;

    public static CommonPublicChatChannelDto from(ChatService chatService, CommonPublicChatChannel chatChannel) {
        CommonPublicChatChannelDto dto = new CommonPublicChatChannelDto();
        dto.id = chatChannel.getId();
        dto.chatChannelDomain = chatChannel.getChatChannelDomain();
        dto.channelTitle = chatService.getChatChannelService(chatChannel).getChannelTitle(chatChannel);
        dto.description = chatChannel.getDescription();
        dto.channelAdminId = chatChannel.getChannelAdminId();
        dto.channelModeratorIds = chatChannel.getChannelModeratorIds();
        return dto;
    }
}
