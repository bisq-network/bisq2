package bisq.api.dto.account;

public record UserDefinedFiatAccountDto(
    String accountName,
    UserDefinedFiatAccountPayloadDto accountPayload
) { }

