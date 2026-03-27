package bisq.api.dto.account.fiat;

import bisq.account.accounts.fiat.BankAccountType;

public record AchTransferAccountPayloadDto(
        String holderName,
        String holderAddress,
        String bankName,
        String routingNr,
        String accountNr,
        BankAccountType bankAccountType
) implements FiatAccountPayloadDto {
}
