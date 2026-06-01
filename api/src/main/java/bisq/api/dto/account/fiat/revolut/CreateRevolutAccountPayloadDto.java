package bisq.api.dto.account.fiat.revolut;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

import java.util.List;

public record CreateRevolutAccountPayloadDto(
        String userName,
        List<String> selectedCurrencyCodes
) implements CreatePaymentAccountPayloadDto {
}
