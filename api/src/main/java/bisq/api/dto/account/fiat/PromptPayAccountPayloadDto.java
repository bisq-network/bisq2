package bisq.api.dto.account.fiat;

public record PromptPayAccountPayloadDto(
        String countryCode,
        String promptPayId
) implements FiatAccountPayloadDto {
}
