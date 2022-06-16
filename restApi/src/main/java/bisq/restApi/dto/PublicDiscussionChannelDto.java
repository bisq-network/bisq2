package bisq.restApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;


@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
@Schema(name = "PublicDiscussionChannel")
public class PublicDiscussionChannelDto extends BaseDto<PublicDiscussionChannelDto> {

    @EqualsAndHashCode.Include
    protected String id;
    private String channelName;
    private String description;
    private String channelAdminId;
    private Set<String> channelModeratorIds;
}
