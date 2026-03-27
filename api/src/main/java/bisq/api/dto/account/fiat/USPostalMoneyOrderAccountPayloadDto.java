package bisq.api.dto.account.fiat;

public record USPostalMoneyOrderAccountPayloadDto(
        String holderName,
        String postalAddress
) implements FiatAccountPayloadDto {
}
