package bisq.api.dto.account.fiat.same_bank;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.common.BankAccountTypeDto;

import java.util.Optional;

public record CreateSameBankAccountPayloadDto(
        String selectedCountryCode,
        String selectedCurrencyCode,
        Optional<String> holderName,
        Optional<String> holderId,
        Optional<String> bankName,
        Optional<String> bankId,
        Optional<String> branchId,
        String accountNr,
        Optional<BankAccountTypeDto> bankAccountType,
        Optional<String> nationalAccountId
) implements CreatePaymentAccountPayloadDto {
}
