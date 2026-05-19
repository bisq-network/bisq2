package bisq.api.dto.account.fiat;

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
