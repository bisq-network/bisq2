package bisq.api.dto.account.crypto.payment_method;

public record CryptoPaymentMethodDto(
        String code,
        String name,
        boolean supportAutoConf,
        String tradeLimitInfo,
        String tradeDuration
) {
}
