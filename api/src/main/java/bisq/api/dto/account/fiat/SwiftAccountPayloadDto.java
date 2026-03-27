package bisq.api.dto.account.fiat;

import java.util.Optional;

public record SwiftAccountPayloadDto(
        String bankCountryCode,
        String beneficiaryName,
        String beneficiaryAccountNr,
        Optional<String> beneficiaryPhone,
        String beneficiaryAddress,
        String selectedCurrencyCode,
        String bankSwiftCode,
        String bankName,
        Optional<String> bankBranch,
        String bankAddress,
        Optional<String> intermediaryBankCountryCode,
        Optional<String> intermediaryBankSwiftCode,
        Optional<String> intermediaryBankName,
        Optional<String> intermediaryBankBranch,
        Optional<String> intermediaryBankAddress,
        Optional<String> additionalInstructions
) implements FiatAccountPayloadDto {
}
