package bisq.account.payment_method;

import lombok.Getter;

@Getter
public enum FiatPaymentMethodChargebackRisk {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int numericLevel;

    FiatPaymentMethodChargebackRisk(int numericLevel) {
        this.numericLevel = numericLevel;
    }

}