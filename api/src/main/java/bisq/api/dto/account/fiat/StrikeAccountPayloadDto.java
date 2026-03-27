package bisq.api.dto.account.fiat;

public record StrikeAccountPayloadDto(
        String countryCode,
        String holderName
) implements FiatAccountPayloadDto {
}
