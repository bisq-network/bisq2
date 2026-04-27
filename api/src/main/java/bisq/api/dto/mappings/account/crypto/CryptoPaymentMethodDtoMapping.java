package bisq.api.dto.mappings.account.crypto;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.api.dto.account.crypto.CryptoPaymentMethodDto;
import bisq.common.asset.CryptoAssetRepository;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigTradeAmountLimits;

public class CryptoPaymentMethodDtoMapping {
    public static CryptoPaymentMethodDto fromBisq2Model(CryptoPaymentMethod paymentMethod) {
        PaymentRail paymentRail = paymentMethod.getPaymentRail();
        String maxTradeLimit = MuSigTradeAmountLimits.getFormattedMaxTradeLimit(paymentRail);
        String restrictions = Res.get("paymentAccounts.summary.tradeLimit", maxTradeLimit) + " / " +
                Res.get("paymentAccounts.summary.tradeDuration", paymentRail.getTradeDuration().getDisplayString());

        return new CryptoPaymentMethodDto(
                paymentMethod.getCode(),
                paymentMethod.getName(),
                paymentMethod.getPaymentRail().name(),
                CryptoAssetRepository.isAutoConfSupported(paymentMethod.getCode()),
                restrictions
        );
    }
}
