package bisq.api.dto.account.fiat;

public record ImpsAccountPayloadDto(
        String holderName,
        String accountNr,
        String ifsc
) implements FiatAccountPayloadDto {
}
