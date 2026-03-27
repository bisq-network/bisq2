package bisq.api.dto.account.fiat;

public record FasterPaymentsAccountPayloadDto(
        String holderName,
        String sortCode,
        String accountNr
) implements FiatAccountPayloadDto {
}
