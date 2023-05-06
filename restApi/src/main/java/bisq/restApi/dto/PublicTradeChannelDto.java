package bisq.restApi.dto;

import bisq.chat.bisqeasy.channel.pub.PublicBisqEasyOfferChatChannel;
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

    public static PublicTradeChannelDto from(PublicBisqEasyOfferChatChannel publicBisqEasyOfferChatChannel) {
        PublicTradeChannelDto dto = new PublicTradeChannelDto();
        dto.channelName = publicBisqEasyOfferChatChannel.getChannelName();
        dto.description = publicBisqEasyOfferChatChannel.getDescription();
        dto.displayString = publicBisqEasyOfferChatChannel.getDisplayString();
        dto.marketDto = MarketDto.from(publicBisqEasyOfferChatChannel.getMarket());
        return dto;
    }
}
