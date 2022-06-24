package bisq.restApi.dto;

import bisq.social.chat.channels.PublicTradeChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Getter
@Setter
@ToString
@Schema(name = "PublicTradeChannel")
public class PublicTradeChannelDto extends BaseDto<PublicTradeChannelDto> {
    @EqualsAndHashCode.Include
    protected String id;

    protected String description;
    protected String displayString;

    @JsonProperty("market")
    protected MarketDto marketDto;

    public static PublicTradeChannelDto from(PublicTradeChannel publicTradeChannel) {
        PublicTradeChannelDto dto = new PublicTradeChannelDto();
        dto.setId(publicTradeChannel.getId());
        dto.setDescription(publicTradeChannel.getDescription());
        dto.setDisplayString(publicTradeChannel.getDisplayString());
        dto.setMarketDto(publicTradeChannel.getMarket().map(MarketDto::from).orElse(null));
        return dto;
    }
}
