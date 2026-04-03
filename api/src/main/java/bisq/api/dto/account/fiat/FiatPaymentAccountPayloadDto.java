package bisq.api.dto.account.fiat;

import bisq.api.dto.account.PaymentAccountPayloadDto;

public interface FiatPaymentAccountPayloadDto extends PaymentAccountPayloadDto {
    FiatPaymentMethodChargebackRiskDto chargebackRisk();

    String paymentMethodName();

    String currency();

    String country();
}
