package bisq.api.dto.account.fiat;

public record Pin4AccountPayloadDto(
        String mobileNr
) implements FiatAccountPayloadDto {
}
