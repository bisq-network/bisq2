package bisq.api.dto.mappings.account.crypto;

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.api.dto.account.crypto.CryptoPaymentMethodDto;
import bisq.api.dto.mappings.account.PaymentMethodDtoHelper;
import bisq.common.asset.CryptoAssetRepository;

public class CryptoPaymentMethodDtoMapping {
    public static CryptoPaymentMethodDto fromBisq2Model(CryptoPaymentMethod paymentMethod) {
<<<<<<< HEAD
=======
        PaymentRail paymentRail = paymentMethod.getPaymentRail();

>>>>>>> b9fa178e25 (Update musig payment method and account models for api app)
        return new CryptoPaymentMethodDto(
                paymentMethod.getCode(),
                paymentMethod.getName(),
                paymentMethod.getPaymentRail().name(),
<<<<<<< HEAD
                CryptoAssetRepository.isAutoConfSupported(paymentMethod.getCode())
=======
                CryptoAssetRepository.isAutoConfSupported(paymentMethod.getCode()),
                PaymentMethodDtoHelper.getTradeLimitInfo(paymentRail),
                PaymentMethodDtoHelper.getTradeDuration(paymentRail)
>>>>>>> b9fa178e25 (Update musig payment method and account models for api app)
        );
    }
}
