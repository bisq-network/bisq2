package bisq.restApi.dto;

import bisq.chat.channels.PublicMarketChannel;
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
    private String id;
    private String description;
    private String displayString;

    @JsonProperty("market")
    private MarketDto marketDto;

    public static PublicTradeChannelDto from(PublicMarketChannel publicMarketChannel) {
        PublicTradeChannelDto dto = new PublicTradeChannelDto();
        dto.id = publicMarketChannel.getId();
        dto.description = publicMarketChannel.getDescription();
        dto.displayString = publicMarketChannel.getDisplayString();
        dto.marketDto = MarketDto.from(publicMarketChannel.getMarket());
        return dto;
    }
}
