package bisq.api.dto.account.fiat;

import java.util.List;

public record WiseAccountPayloadDto(
        List<String> selectedCurrencyCodes,
        String holderName,
        String email
) implements FiatAccountPayloadDto {
}
