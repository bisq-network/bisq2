package bisq.restApi.dto;

import bisq.common.currency.Market;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(name = "Market")
@Getter
@EqualsAndHashCode
public final class MarketDto {
    private String baseCurrencyCode;
    private String quoteCurrencyCode;
    private String baseCurrencyName;
    private String quoteCurrencyName;

    public static MarketDto from(Market market) {
        MarketDto dto = new MarketDto();
        dto.baseCurrencyCode = market.getBaseCurrencyCode();
        dto.quoteCurrencyCode = market.getQuoteCurrencyCode();
        dto.baseCurrencyName = market.getBaseCurrencyName();
        dto.quoteCurrencyName = market.getQuoteCurrencyName();
        return dto;
    }
}


