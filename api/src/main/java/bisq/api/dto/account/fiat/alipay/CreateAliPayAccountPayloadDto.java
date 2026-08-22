package bisq.api.dto.account.fiat.alipay;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

public record CreateAliPayAccountPayloadDto(
        String accountNr
) implements CreatePaymentAccountPayloadDto {
}
