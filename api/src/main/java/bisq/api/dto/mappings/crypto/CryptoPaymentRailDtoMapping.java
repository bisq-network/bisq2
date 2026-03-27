package bisq.api.dto.mappings.crypto;

import bisq.api.dto.account.crypto.CryptoPaymentRailDto;

public class CryptoPaymentRailDtoMapping {
    public static CryptoPaymentRailDto fromMoneroAccountType() {
        return CryptoPaymentRailDto.MONERO;
    }

    public static CryptoPaymentRailDto fromOtherCryptoAssetAccountType() {
        return CryptoPaymentRailDto.OTHER_CRYPTO_ASSET;
    }
}
