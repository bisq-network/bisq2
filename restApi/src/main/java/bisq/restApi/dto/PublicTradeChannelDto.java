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
    private String channelId;
    private String description;
    private String channelTitle;

    @JsonProperty("market")
    private MarketDto marketDto;

    public static PublicTradeChannelDto from(BisqEasyPublicChatChannel bisqEasyPublicChatChannel) {
        PublicTradeChannelDto dto = new PublicTradeChannelDto();
        dto.channelId = bisqEasyPublicChatChannel.getId();
        dto.description = bisqEasyPublicChatChannel.getDescription();
        dto.channelTitle = bisqEasyPublicChatChannel.getChannelTitle();
        dto.marketDto = MarketDto.from(bisqEasyPublicChatChannel.getMarket());
        return dto;
    }
}
