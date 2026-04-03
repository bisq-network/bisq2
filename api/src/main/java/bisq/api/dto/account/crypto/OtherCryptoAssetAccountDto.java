package bisq.api.dto.account.crypto;

import bisq.api.dto.account.PaymentAccountDto;

public record OtherCryptoAssetAccountDto(
        String accountName,
        CryptoPaymentRailDto paymentRail,
        OtherCryptoAssetAccountPayloadDto accountPayload,
        String creationDate,
        String tradeLimitInfo,
        String tradeDuration
) implements PaymentAccountDto {

    public OtherCryptoAssetAccountDto {
        if (paymentRail != CryptoPaymentRailDto.OTHER_CRYPTO_ASSET) {
            throw new IllegalArgumentException("paymentRail must be OTHER_CRYPTO_ASSET");
        }
    }

    public OtherCryptoAssetAccountDto(String accountName,
                                      OtherCryptoAssetAccountPayloadDto accountPayload,
                                      String creationDate,
                                      String tradeLimitInfo,
                                      String tradeDuration) {
        this(accountName, CryptoPaymentRailDto.OTHER_CRYPTO_ASSET, accountPayload, creationDate, tradeLimitInfo, tradeDuration);
    }
}
