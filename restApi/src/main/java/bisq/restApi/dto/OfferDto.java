package bisq.restApi.dto;

import bisq.account.protocol.SwapProtocolType;
import bisq.offer.Offer;
import bisq.offer.spec.Direction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Data
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Schema(name = "Offer")
public class OfferDto extends BaseDto<OfferDto> {

    @EqualsAndHashCode.Include
    protected String id;
    protected long date;
    protected String makerNodeId;

    protected MarketDto market;
    protected Direction direction;
    protected long baseAmount;

    protected long fixPrice;
    protected List<SwapProtocolType> swapProtocolTypes; // SwapProtocolType can be used directly

    public OfferDto loadFieldsFrom(Offer offer) {
        // map the easy stuff
        super.loadFieldsFrom(offer);
        market = new MarketDto().loadFieldsFrom(offer.getMarket());
        return this;
    }

}
