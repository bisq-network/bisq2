package bisq.api.dto.account.fiat;

public record CashByMailAccountPayloadDto(
        String selectedCurrencyCode,
        String postalAddress,
        String contact,
        String extraInfo
) implements FiatAccountPayloadDto {
}
