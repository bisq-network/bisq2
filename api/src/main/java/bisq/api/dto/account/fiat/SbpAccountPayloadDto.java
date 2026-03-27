package bisq.api.dto.account.fiat;

public record SbpAccountPayloadDto(
        String holderName,
        String mobileNumber,
        String bankName
) implements FiatAccountPayloadDto {
}
