package bisq.api.dto.account.fiat;

public record AmazonGiftCardAccountPayloadDto(
        String countryCode,
        String selectedCurrencyCode,
        String emailOrMobileNr
) implements FiatAccountPayloadDto {
}
