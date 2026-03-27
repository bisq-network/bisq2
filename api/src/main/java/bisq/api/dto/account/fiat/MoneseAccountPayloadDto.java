package bisq.api.dto.account.fiat;

import java.util.List;

public record MoneseAccountPayloadDto(
        List<String> selectedCurrencyCodes,
        String holderName,
        String mobileNr
) implements FiatAccountPayloadDto {
}
