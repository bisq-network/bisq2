package bisq.api.dto.mappings.account;

import bisq.account.payment_method.PaymentRail;
import bisq.mu_sig.MuSigTradeAmountLimits;

public final class PaymentMethodDtoHelper {
    private PaymentMethodDtoHelper() {
    }

    public static String getTradeLimitInfo(PaymentRail paymentRail) {
        return MuSigTradeAmountLimits.getFormattedMaxTradeLimitInUsd(paymentRail);
    }

    public static String getTradeDuration(PaymentRail paymentRail) {
        return paymentRail.getTradeDuration().getDisplayString();
    }
}
