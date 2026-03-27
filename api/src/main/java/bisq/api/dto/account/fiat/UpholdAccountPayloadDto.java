package bisq.api.dto.account.fiat;

import java.util.List;

public record UpholdAccountPayloadDto(
        List<String> selectedCurrencyCodes,
        String holderName,
        String accountId
) implements FiatAccountPayloadDto {
}
