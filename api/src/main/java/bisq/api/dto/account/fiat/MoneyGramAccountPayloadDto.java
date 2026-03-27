package bisq.api.dto.account.fiat;

import java.util.List;

public record MoneyGramAccountPayloadDto(
        String countryCode,
        List<String> selectedCurrencyCodes,
        String holderName,
        String email,
        String state
) implements FiatAccountPayloadDto {
}
