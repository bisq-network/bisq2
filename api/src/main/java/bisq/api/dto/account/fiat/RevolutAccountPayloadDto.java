package bisq.api.dto.account.fiat;

import java.util.List;

public record RevolutAccountPayloadDto(
        String userName,
        List<String> selectedCurrencyCodes
) implements FiatAccountPayloadDto {
}
