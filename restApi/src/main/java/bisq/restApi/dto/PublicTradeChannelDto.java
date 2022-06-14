package bisq.restApi.dto;

import bisq.social.chat.channels.PublicTradeChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Data
@Schema(name = "PublicTradeChannel")
public class PublicTradeChannelDto extends BaseDto<PublicTradeChannelDto> {
    @EqualsAndHashCode.Include
    protected String id;

    protected String description;
    protected String channelAdminId;
    protected String displayString;

    @JsonProperty("market")
    protected MarketDto marketDto;

    public PublicTradeChannelDto read(PublicTradeChannel publicTradeChannel) {
        super.read(publicTradeChannel);
        setMarketDto(publicTradeChannel.getMarket().map(market -> new MarketDto().read(market)).orElse(null));
        return this;
    }
}
