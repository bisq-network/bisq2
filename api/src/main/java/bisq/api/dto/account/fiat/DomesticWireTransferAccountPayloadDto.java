package bisq.api.dto.account.fiat;

public record DomesticWireTransferAccountPayloadDto(
        String holderName,
        String holderAddress,
        String bankName,
        String routingNr,
        String accountNr
) implements FiatAccountPayloadDto {
}
