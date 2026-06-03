package bisq.api.dto.account.fiat.zelle;

import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

public record ZelleAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        String currency,
        String country,
        String holderName,
        String emailOrMobileNr
) implements FiatPaymentAccountPayloadDto {
}
