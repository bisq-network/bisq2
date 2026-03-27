package bisq.api.dto.account.fiat;

public record BizumAccountPayloadDto(
        String countryCode,
        String mobileNr
) implements FiatAccountPayloadDto {
}
