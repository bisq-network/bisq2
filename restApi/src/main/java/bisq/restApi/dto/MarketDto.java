package bisq.restApi.dto;

import bisq.common.currency.Market;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(name = "Market")
@Data
@EqualsAndHashCode(callSuper = false)
public class MarketDto extends BaseDto<MarketDto> {

    protected String baseCurrencyCode;
    protected String quoteCurrencyCode;
    protected String baseCurrencyName;
    protected String quoteCurrencyName;

    public MarketDto loadFieldsFrom(Market market) {
        setBaseCurrencyCode(market.baseCurrencyCode());
        setQuoteCurrencyCode(market.quoteCurrencyCode());
        setBaseCurrencyName(market.baseCurrencyName());
        setQuoteCurrencyName(market.quoteCurrencyName());
        return this;
    }
}


