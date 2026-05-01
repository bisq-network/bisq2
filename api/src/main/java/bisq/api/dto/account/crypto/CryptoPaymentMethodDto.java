package bisq.api.dto.account.crypto;

public record CryptoPaymentMethodDto(
        String code,
        String name,
        String category,
        boolean supportAutoConf,
        String tradeLimitInfo,
        String tradeDuration
) {
}
