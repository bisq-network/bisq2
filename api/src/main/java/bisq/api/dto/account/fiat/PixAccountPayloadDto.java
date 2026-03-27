package bisq.api.dto.account.fiat;

public record PixAccountPayloadDto(
        String countryCode,
        String holderName,
        String pixKey
) implements FiatAccountPayloadDto {
}
