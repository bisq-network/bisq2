package bisq.api.dto.account.fiat;

public record WeChatPayAccountPayloadDto(
        String accountNr
) implements FiatAccountPayloadDto {
}
