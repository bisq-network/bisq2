package bisq.api.dto.account.fiat;

public record ZelleAccountPayloadDto(
        String holderName,
        String emailOrMobileNr
) implements FiatAccountPayloadDto {
}
