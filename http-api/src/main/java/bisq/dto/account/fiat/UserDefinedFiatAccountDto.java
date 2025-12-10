package bisq.dto.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentRail;

public record UserDefinedFiatAccountDto(
    String accountName,
    FiatPaymentRail paymentRail,
    UserDefinedFiatAccountPayloadDto accountPayload
) implements FiatAccountDto { }

