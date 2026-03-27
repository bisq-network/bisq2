package bisq.api.dto.account.fiat;

public record AliPayAccountPayloadDto(
        String accountNr
) implements FiatAccountPayloadDto {
}
