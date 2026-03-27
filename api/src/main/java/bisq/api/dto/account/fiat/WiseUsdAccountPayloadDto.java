package bisq.api.dto.account.fiat;

public record WiseUsdAccountPayloadDto(
        String countryCode,
        String holderName,
        String email,
        String beneficiaryAddress
) implements FiatAccountPayloadDto {
}
