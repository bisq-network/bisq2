package bisq.api.dto.account.crypto;

public record CryptoPaymentMethodDto(
        String code,
        String name,
        String category,
<<<<<<< HEAD
        boolean supportAutoConf
=======
        boolean supportAutoConf,
        String tradeLimitInfo,
        String tradeDuration
>>>>>>> b9fa178e25 (Update musig payment method and account models for api app)
) {
}
