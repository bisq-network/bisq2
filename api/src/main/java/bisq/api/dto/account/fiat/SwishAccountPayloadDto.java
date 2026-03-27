package bisq.api.dto.account.fiat;

public record SwishAccountPayloadDto(
        String countryCode,
        String holderName,
        String mobileNr
) implements FiatAccountPayloadDto {
}
