package bisq.api.dto.account.fiat;

import bisq.api.dto.account.PaymentAccountDto;

public record ZelleAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        ZelleAccountPayloadDto accountPayload,
        Long creationDate
) implements PaymentAccountDto {

    public ZelleAccountDto {
        if (paymentRail != FiatPaymentRailDto.ZELLE) {
            throw new IllegalArgumentException("paymentRail must be ZELLE");
        }
    }

    public ZelleAccountDto(String accountName,
                           ZelleAccountPayloadDto accountPayload,
                           Long creationDate) {
        this(accountName, FiatPaymentRailDto.ZELLE, accountPayload, creationDate);
    }
}
