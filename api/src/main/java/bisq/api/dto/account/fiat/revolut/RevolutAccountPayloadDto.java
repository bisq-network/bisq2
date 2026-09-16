package bisq.api.dto.account.fiat.revolut;

import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

import java.util.List;

public record RevolutAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        List<FiatCurrencyDto> selectedCurrencies,
        String userName
) implements FiatPaymentAccountPayloadDto {
}
