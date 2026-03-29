package bisq.api.dto.mappings.account.crypto;

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.api.dto.account.crypto.CryptoPaymentMethodDto;

public class CryptoPaymentMethodDtoMapping {
    public static CryptoPaymentMethodDto fromBisq2Model(CryptoPaymentMethod paymentMethod) {
        return new CryptoPaymentMethodDto(
                paymentMethod.getCode(),
                paymentMethod.getName(),
                paymentMethod.getPaymentRail().name()
        );
    }
}
