package bisq.api.dto.account.fiat;

import java.util.List;

public record WiseAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        List<FiatCurrencyDto> selectedCurrencies,
        String holderName,
        String email
) implements FiatPaymentAccountPayloadDto {
}
