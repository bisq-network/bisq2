package bisq.api.dto.mappings.account;

import bisq.account.payment_method.PaymentRail;
import bisq.common.monetary.Fiat;
import bisq.offer.mu_sig.use_case.create_offer.amount.limits.PaymentMethodBasedAmountLimitsProvider;
import bisq.presentation.formatters.AmountFormatter;

public final class PaymentMethodDtoMappingHelper {
    private PaymentMethodDtoMappingHelper() {
    }

    public static String getTradeLimitInfo(PaymentRail paymentRail) {
        Fiat maxTradeLimitInUsd = PaymentMethodBasedAmountLimitsProvider.evaluateLimitInUsd(paymentRail);
        return AmountFormatter.formatQuoteAmount(maxTradeLimitInUsd);
    }

    public static String getTradeDuration(PaymentRail paymentRail) {
        return paymentRail.getTradeDuration().getDisplayString();
    }
}
