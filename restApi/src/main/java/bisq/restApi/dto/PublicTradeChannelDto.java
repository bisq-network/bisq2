package bisq.restApi.dto;

import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
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

    public static PublicTradeChannelDto from(BisqEasyPublicChatChannel bisqEasyPublicChatChannel) {
        PublicTradeChannelDto dto = new PublicTradeChannelDto();
        dto.channelName = bisqEasyPublicChatChannel.getChannelName();
        dto.description = bisqEasyPublicChatChannel.getDescription();
        dto.displayString = bisqEasyPublicChatChannel.getDisplayString();
        dto.marketDto = MarketDto.from(bisqEasyPublicChatChannel.getMarket());
        return dto;
    }
}
