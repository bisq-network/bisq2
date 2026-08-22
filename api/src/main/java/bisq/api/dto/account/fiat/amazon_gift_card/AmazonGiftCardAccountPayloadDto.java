package bisq.api.dto.account.fiat.amazon_gift_card;

import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

public record AmazonGiftCardAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        String emailOrMobileNr
) implements FiatPaymentAccountPayloadDto {
}
