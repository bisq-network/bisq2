package bisq.api.dto.mappings.account;

import bisq.account.payment_method.PaymentRail;

public final class PaymentMethodDtoMappingHelper {
    private PaymentMethodDtoMappingHelper() {
    }

    public static String getTradeLimitInfo(PaymentRail paymentRail) {
        //todo MuSigTradeAmountLimits has been removed
        return "TODO"; // MuSigTradeAmountLimits.getFormattedMaxTradeLimitInUsd(paymentRail);
    }

    public static String getTradeDuration(PaymentRail paymentRail) {
        return paymentRail.getTradeDuration().getDisplayString();
    }
}
