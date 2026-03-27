package bisq.api.dto.account.crypto;

public record CryptoPaymentMethodItemDto(
        String code,
        String name,
        String category
) {
}
