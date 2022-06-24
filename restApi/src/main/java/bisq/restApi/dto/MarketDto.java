package bisq.restApi.dto;

import bisq.common.currency.Market;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Schema(name = "Market")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MarketDto extends BaseDto<MarketDto> {

    protected String baseCurrencyCode;
    protected String quoteCurrencyCode;
    protected String baseCurrencyName;
    protected String quoteCurrencyName;

    public static MarketDto from(Market market) {
        MarketDto dto = new MarketDto();
        dto.setBaseCurrencyCode(market.baseCurrencyCode());
        dto.setQuoteCurrencyCode(market.quoteCurrencyCode());
        dto.setBaseCurrencyName(market.baseCurrencyName());
        dto.setQuoteCurrencyName(market.quoteCurrencyName());
        return dto;
    }
}


