package bisq.api.dto.account.fiat.sepa;

import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

import java.util.List;

public record SepaAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        List<CountryDto> acceptedCountries,
        String holderName,
        String iban,
        String bic
) implements FiatPaymentAccountPayloadDto {
}
