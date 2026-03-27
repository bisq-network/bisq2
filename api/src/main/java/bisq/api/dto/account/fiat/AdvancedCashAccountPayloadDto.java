package bisq.api.dto.account.fiat;

import java.util.List;

public record AdvancedCashAccountPayloadDto(
        List<String> selectedCurrencyCodes,
        String accountNr
) implements FiatAccountPayloadDto {
}
