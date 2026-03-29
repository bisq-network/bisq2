package bisq.api.dto.account.crypto;

import bisq.api.dto.account.PaymentAccountDto;

public record OtherCryptoAssetAccountDto(
        String accountName,
        CryptoPaymentRailDto paymentRail,
        OtherCryptoAssetAccountPayloadDto accountPayload,
        Long creationDate
) implements PaymentAccountDto {

    public OtherCryptoAssetAccountDto {
        if (paymentRail != CryptoPaymentRailDto.OTHER_CRYPTO_ASSET) {
            throw new IllegalArgumentException("paymentRail must be OTHER_CRYPTO_ASSET");
        }
    }

    public OtherCryptoAssetAccountDto(String accountName,
                                      OtherCryptoAssetAccountPayloadDto accountPayload,
                                      Long creationDate) {
        this(accountName, CryptoPaymentRailDto.OTHER_CRYPTO_ASSET, accountPayload, creationDate);
    }
}
