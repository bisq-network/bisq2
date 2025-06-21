package bisq.account.payment_method;

import lombok.Getter;

@Getter
public enum FiatPaymentMethodChargebackRisk {
    LOW,
    MEDIUM,
    HIGH
}