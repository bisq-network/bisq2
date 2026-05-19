package bisq.api.dto.account.fiat.money_gram;

import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

import java.util.List;

public record MoneyGramAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        CountryDto country,
        List<FiatCurrencyDto> selectedCurrencies,
        String holderName,
        String email,
        String state
) implements FiatPaymentAccountPayloadDto {
}
