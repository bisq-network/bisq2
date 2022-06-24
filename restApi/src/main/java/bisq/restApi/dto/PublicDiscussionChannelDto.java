package bisq.restApi.dto;

import bisq.social.chat.channels.PublicDiscussionChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;


@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@ToString
@Schema(name = "PublicDiscussionChannel")
public class PublicDiscussionChannelDto extends BaseDto<PublicDiscussionChannelDto> {

    @EqualsAndHashCode.Include
    protected String id;
    private String channelName;
    private String description;
    private String channelAdminId;
    private Set<String> channelModeratorIds;

    public static PublicDiscussionChannelDto from(PublicDiscussionChannel publicDiscussionChannel) {
        PublicDiscussionChannelDto dto = new PublicDiscussionChannelDto();
        dto.setId(publicDiscussionChannel.getId());
        dto.setChannelName(publicDiscussionChannel.getChannelName());
        dto.setDescription(publicDiscussionChannel.getDescription());
        dto.setChannelAdminId(publicDiscussionChannel.getChannelAdminId());
        dto.setChannelModeratorIds(publicDiscussionChannel.getChannelModeratorIds());
        return dto;
    }
}
