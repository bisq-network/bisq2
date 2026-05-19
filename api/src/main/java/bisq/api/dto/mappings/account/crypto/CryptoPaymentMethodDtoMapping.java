package bisq.api.dto.mappings.account.crypto;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.api.dto.account.crypto.CryptoPaymentMethodDto;
import bisq.api.dto.mappings.account.PaymentMethodDtoMappingHelper;
import bisq.common.asset.CryptoAssetRepository;

public class CryptoPaymentMethodDtoMapping {
    public static CryptoPaymentMethodDto fromBisq2Model(CryptoPaymentMethod paymentMethod) {
        PaymentRail paymentRail = paymentMethod.getPaymentRail();

        return new CryptoPaymentMethodDto(
                paymentMethod.getCode(),
                paymentMethod.getName(),
                CryptoAssetRepository.isAutoConfSupported(paymentMethod.getCode()),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(paymentRail),
                PaymentMethodDtoMappingHelper.getTradeDuration(paymentRail)
        );
    }
}
