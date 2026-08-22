package bisq.api.dto.account.fiat.cash_deposit;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.common.BankAccountTypeDto;

import java.util.Optional;

public record CreateCashDepositAccountPayloadDto(
        String selectedCountryCode,
        String selectedCurrencyCode,
        String holderName,
        Optional<String> holderId,
        String bankName,
        Optional<String> bankId,
        Optional<String> branchId,
        String accountNr,
        Optional<BankAccountTypeDto> bankAccountType,
        Optional<String> nationalAccountId,
        Optional<String> requirements
) implements CreatePaymentAccountPayloadDto {
}
