package bisq.api.dto.account.fiat.money_gram;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

import java.util.List;

public record CreateMoneyGramAccountPayloadDto(
        String selectedCountryCode,
        List<String> selectedCurrencyCodes,
        String holderName,
        String email,
        String state
) implements CreatePaymentAccountPayloadDto {
}
