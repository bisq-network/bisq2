package bisq.api.dto.account.fiat.ach;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.common.BankAccountTypeDto;

public record CreateAchTransferAccountPayloadDto(
        String holderName,
        String holderAddress,
        String bankName,
        String routingNr,
        String accountNr,
        BankAccountTypeDto bankAccountType
) implements CreatePaymentAccountPayloadDto {
}
