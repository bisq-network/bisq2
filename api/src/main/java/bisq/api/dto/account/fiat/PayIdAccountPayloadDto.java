package bisq.api.dto.account.fiat;

public record PayIdAccountPayloadDto(
        String holderName,
        String payId
) implements FiatAccountPayloadDto {
}
