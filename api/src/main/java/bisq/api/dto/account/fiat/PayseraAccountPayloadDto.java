package bisq.api.dto.account.fiat;

import java.util.List;

public record PayseraAccountPayloadDto(
        List<String> selectedCurrencyCodes,
        String email
) implements FiatAccountPayloadDto {
}
