package bisq.api.dto.account.fiat;

public record F2FAccountPayloadDto(
        String countryCode,
        String selectedCurrencyCode,
        String city,
        String contact,
        String extraInfo
) implements FiatAccountPayloadDto {
}
