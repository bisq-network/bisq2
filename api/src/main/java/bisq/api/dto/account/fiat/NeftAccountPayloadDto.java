package bisq.api.dto.account.fiat;

public record NeftAccountPayloadDto(
        String holderName,
        String accountNr,
        String ifsc
) implements FiatAccountPayloadDto {
}
