package bisq.api.dto.account.fiat.wise;

import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

import java.util.List;

public record WiseAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        List<FiatCurrencyDto> selectedCurrencies,
        String holderName,
        String email
) implements FiatPaymentAccountPayloadDto {
}
