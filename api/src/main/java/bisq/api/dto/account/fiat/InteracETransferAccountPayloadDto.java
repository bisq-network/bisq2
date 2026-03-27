package bisq.api.dto.account.fiat;

public record InteracETransferAccountPayloadDto(
        String holderName,
        String email,
        String question,
        String answer
) implements FiatAccountPayloadDto {
}
