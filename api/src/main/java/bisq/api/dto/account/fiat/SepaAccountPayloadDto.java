package bisq.api.dto.account.fiat;

import java.util.List;

public record SepaAccountPayloadDto(
        String holderName,
        String iban,
        String bic,
        String countryCode,
        List<String> acceptedCountryCodes
) implements FiatAccountPayloadDto {
}
