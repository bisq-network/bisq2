package bisq.api.dto.account.fiat;


public record UpiAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        UpiAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public UpiAccountDto {
        if (paymentRail != FiatPaymentRailDto.UPI) {
            throw new IllegalArgumentException("paymentRail must be UPI");
        }
    }

    public UpiAccountDto(String accountName,
                         UpiAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.UPI, accountPayload);
    }
}
