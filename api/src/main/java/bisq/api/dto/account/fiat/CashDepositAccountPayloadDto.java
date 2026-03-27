package bisq.api.dto.account.fiat;

import bisq.account.accounts.fiat.BankAccountType;

import java.util.Optional;

public record CashDepositAccountPayloadDto(
        String countryCode,
        String selectedCurrencyCode,
        Optional<String> holderName,
        Optional<String> holderTaxId,
        Optional<String> bankName,
        Optional<String> bankId,
        Optional<String> branchId,
        String accountNr,
        Optional<BankAccountType> bankAccountType,
        Optional<String> nationalAccountId,
        Optional<String> requirements
) implements FiatAccountPayloadDto {
}
