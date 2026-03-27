package bisq.api.dto.account.fiat;

public record UpiAccountPayloadDto(
        String countryCode,
        String virtualPaymentAddress
) implements FiatAccountPayloadDto {
}
