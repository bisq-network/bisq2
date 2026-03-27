package bisq.api.dto.account.fiat;

public record MercadoPagoAccountPayloadDto(
        String holderName,
        String holderId
) implements FiatAccountPayloadDto {
}
