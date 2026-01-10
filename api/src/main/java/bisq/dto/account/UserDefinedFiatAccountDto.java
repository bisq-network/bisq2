package bisq.dto.account;

public record UserDefinedFiatAccountDto(
    String accountName,
    UserDefinedFiatAccountPayloadDto accountPayload
) { }

