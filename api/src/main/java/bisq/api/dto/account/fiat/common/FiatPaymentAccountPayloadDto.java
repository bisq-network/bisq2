package bisq.api.dto.account.fiat.common;

import bisq.api.dto.account.PaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

public interface FiatPaymentAccountPayloadDto extends PaymentAccountPayloadDto {
    FiatPaymentMethodChargebackRiskDto chargebackRisk();

    String paymentMethodName();
}
