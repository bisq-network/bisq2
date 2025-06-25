package bisq.account.payment_method;

import bisq.i18n.Res;
import lombok.Getter;

@Getter
public enum FiatPaymentMethodChargebackRisk {
    VERY_LOW,
    LOW,
    MODERATE;

    public String getDisplayString() {
        return switch (this) {
            case VERY_LOW -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.veryLow");
            case LOW -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.low");
            case MODERATE -> Res.get("user.paymentAccounts.createAccount.paymentMethod.risk.moderate");
        };
    }
}