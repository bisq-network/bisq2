package bisq.api.dto.account.fiat;

public record SatispayAccountPayloadDto(
        String holderName,
        String mobileNr
) implements FiatAccountPayloadDto {
}
