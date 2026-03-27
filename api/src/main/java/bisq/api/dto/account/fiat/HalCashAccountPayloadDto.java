package bisq.api.dto.account.fiat;

public record HalCashAccountPayloadDto(
        String mobileNr
) implements FiatAccountPayloadDto {
}
