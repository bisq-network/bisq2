package bisq.dto.account.fiat;

public record UserDefinedFiatAccountPayloadDto(
        String accountData
) implements FiatAccountPayloadDto { }
