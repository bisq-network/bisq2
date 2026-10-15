package bisq.api.dto.account.fiat.sepa;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

import java.util.List;

public record CreateSepaAccountPayloadDto(
        String selectedCountryCode,
        List<String> acceptedCountryCodes,
        String holderName,
        String iban,
        String bic
) implements CreatePaymentAccountPayloadDto {
}
