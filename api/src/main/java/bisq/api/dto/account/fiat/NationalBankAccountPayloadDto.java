package bisq.api.dto.account.fiat;

import bisq.account.accounts.fiat.BankAccountType;

import java.util.Optional;

public record NationalBankAccountPayloadDto(
        String countryCode,
        String selectedCurrencyCode,
        Optional<String> holderName,
        Optional<String> holderId,
        Optional<String> bankName,
        Optional<String> bankId,
        Optional<String> branchId,
        String accountNr,
        Optional<BankAccountType> bankAccountType,
        Optional<String> nationalAccountId
) implements FiatAccountPayloadDto {
}
