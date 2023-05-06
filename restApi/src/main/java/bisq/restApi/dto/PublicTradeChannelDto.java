package bisq.restApi.dto;

import bisq.chat.trade.channel.pub.PublicTradeChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@ToString
@Schema(name = "PublicTradeChannel")
public final class PublicTradeChannelDto {
    @EqualsAndHashCode.Include
    private String channelName;
    private String description;
    private String displayString;

    @JsonProperty("market")
    private MarketDto marketDto;

    public static PublicTradeChannelDto from(PublicTradeChannel publicTradeChannel) {
        PublicTradeChannelDto dto = new PublicTradeChannelDto();
        dto.channelName = publicTradeChannel.getChannelName();
        dto.description = publicTradeChannel.getDescription();
        dto.displayString = publicTradeChannel.getDisplayString();
        dto.marketDto = MarketDto.from(publicTradeChannel.getMarket());
        return dto;
    }
}
