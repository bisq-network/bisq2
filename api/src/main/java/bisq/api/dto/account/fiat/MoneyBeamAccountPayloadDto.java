package bisq.api.dto.account.fiat;

public record MoneyBeamAccountPayloadDto(
        String countryCode,
        String holderName,
        String emailOrMobileNr
) implements FiatAccountPayloadDto {
}
