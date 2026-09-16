package bisq.api.dto.account.fiat.user_defined;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

public record CreateUserDefinedFiatAccountPayloadDto(
        String accountData
) implements CreatePaymentAccountPayloadDto {
}
