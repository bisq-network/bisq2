package bisq.api.dto.account.fiat.create;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

public record CreateUserDefinedFiatAccountPayloadDto(
        String accountData
) implements CreatePaymentAccountPayloadDto {
}
