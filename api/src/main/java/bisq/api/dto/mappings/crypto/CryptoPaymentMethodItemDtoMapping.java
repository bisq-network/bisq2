package bisq.api.dto.mappings.crypto;

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.api.dto.account.crypto.CryptoPaymentMethodItemDto;

public class CryptoPaymentMethodItemDtoMapping {
    public static CryptoPaymentMethodItemDto fromBisq2Model(CryptoPaymentMethod paymentMethod) {
        return new CryptoPaymentMethodItemDto(
                paymentMethod.getCode(),
                paymentMethod.getName(),
                paymentMethod.getPaymentRail().name()
        );
    }
}
