package bisq.account.payment_method;

import bisq.i18n.Res;
import lombok.Getter;

@Getter
public enum FiatPaymentMethodChargebackRisk {
    VERY_LOW,
    LOW,
    MEDIUM,
    MODERATE;

    @Override
    public String toString() {
        return switch (this) {
            case VERY_LOW -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.veryLow");
            case LOW -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.low");
            case MEDIUM -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.medium");
            case MODERATE -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.moderate");
        };
    }
}