package bisq.api.dto.account.fiat;

import bisq.api.dto.account.PaymentAccountDto;

public record ZelleAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        ZelleAccountPayloadDto accountPayload,
        String creationDate,
        String tradeLimitInfo,
        String tradeDuration
) implements PaymentAccountDto {

    public ZelleAccountDto {
        if (paymentRail != FiatPaymentRailDto.ZELLE) {
            throw new IllegalArgumentException("paymentRail must be ZELLE");
        }
    }

    public ZelleAccountDto(String accountName,
                           ZelleAccountPayloadDto accountPayload,
                           String creationDate,
                           String tradeLimitInfo,
                           String tradeDuration) {
        this(accountName, FiatPaymentRailDto.ZELLE, accountPayload, creationDate, tradeLimitInfo, tradeDuration);
    }
}
