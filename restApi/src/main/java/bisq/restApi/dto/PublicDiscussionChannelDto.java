package bisq.restApi.dto;

import bisq.chat.channel.ChannelDomain;
import bisq.chat.channel.PublicChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;


@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@ToString
@Schema(name = "PublicDiscussionChannel")
public final class PublicDiscussionChannelDto {
    @EqualsAndHashCode.Include
    private String id;
    private ChannelDomain channelDomain;
    private String channelName;
    private String description;
    private String channelAdminId;
    private List<String> channelModeratorIds;

    public static PublicDiscussionChannelDto from(PublicChannel publicDiscussionChannel) {
        PublicDiscussionChannelDto dto = new PublicDiscussionChannelDto();
        dto.id = publicDiscussionChannel.getId();
        dto.channelDomain = publicDiscussionChannel.getChannelDomain();
        dto.channelName = publicDiscussionChannel.getChannelName();
        dto.description = publicDiscussionChannel.getDescription();
        dto.channelAdminId = publicDiscussionChannel.getChannelAdminId();
        dto.channelModeratorIds = publicDiscussionChannel.getChannelModeratorIds();
        return dto;
    }
}
