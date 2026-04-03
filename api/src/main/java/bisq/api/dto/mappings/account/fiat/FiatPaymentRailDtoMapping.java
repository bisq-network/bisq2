package bisq.api.dto.mappings.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.api.dto.account.fiat.FiatPaymentRailDto;

public class FiatPaymentRailDtoMapping {
    public static FiatPaymentRail toBisq2Model(FiatPaymentRailDto value) {
        if (value == null) {
            return null;
        }
        return FiatPaymentRail.valueOf(value.name());
    }

    public static FiatPaymentRailDto fromBisq2Model(FiatPaymentRail value) {
        if (value == null) {
            return null;
        }
        return FiatPaymentRailDto.valueOf(value.name());
    }
}
