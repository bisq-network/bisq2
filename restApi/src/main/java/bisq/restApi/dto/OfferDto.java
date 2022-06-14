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
    protected long date; //long ???
    protected String makeNodeId; // instead of network ID

    protected MarketDto market; // why has Market no ID?
    protected Direction direction;
    protected long baseAmount;

    protected long fixPrice; //price is a long and is fixed?? what's the date when price was valid?
    protected List<SwapProtocolType> swapProtocolTypes; // SwapProtocolType can be used directly

    public OfferDto read(Offer offer) {
        // map the easy stuff
        super.read(offer);
        market = new MarketDto().read(offer.getMarket());
        return this;
    }

}
