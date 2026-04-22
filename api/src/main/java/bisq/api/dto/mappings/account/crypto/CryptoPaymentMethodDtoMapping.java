package bisq.api.dto.mappings.account.crypto;

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.api.dto.account.crypto.CryptoPaymentMethodDto;
import bisq.common.asset.CryptoAssetRepository;

public class CryptoPaymentMethodDtoMapping {
    public static CryptoPaymentMethodDto fromBisq2Model(CryptoPaymentMethod paymentMethod) {
        return new CryptoPaymentMethodDto(
                paymentMethod.getCode(),
                paymentMethod.getName(),
                paymentMethod.getPaymentRail().name(),
                CryptoAssetRepository.isAutoConfSupported(paymentMethod.getCode())
        );
    }
}
