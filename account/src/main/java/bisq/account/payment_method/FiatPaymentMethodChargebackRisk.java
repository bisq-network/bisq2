package bisq.account.payment_method;

import bisq.i18n.Res;
import lombok.Getter;

//todo Introduce a generic RiskLevel to cover also double-spend risks, custodian risk,...
@Getter
public enum FiatPaymentMethodChargebackRisk {
    VERY_LOW,
    LOW,
    MEDIUM,
    MODERATE;

    @Override
    public String toString() {
        return switch (this) {
            case VERY_LOW -> Res.get("paymentAccounts.createAccount.paymentMethod.risk.veryLow");
            case LOW -> Res.get("paymentAccounts.createAccount.paymentMethod.risk.low");
            case MEDIUM -> Res.get("paymentAccounts.createAccount.paymentMethod.risk.medium");
            case MODERATE -> Res.get("paymentAccounts.createAccount.paymentMethod.risk.moderate");
        };
    }
}