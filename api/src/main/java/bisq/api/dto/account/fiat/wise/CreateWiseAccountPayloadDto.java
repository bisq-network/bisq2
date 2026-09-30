package bisq.api.dto.account.fiat.wise;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

import java.util.List;

public record CreateWiseAccountPayloadDto(
        List<String> selectedCurrencyCodes,
        String holderName,
        String email
) implements CreatePaymentAccountPayloadDto {
}
