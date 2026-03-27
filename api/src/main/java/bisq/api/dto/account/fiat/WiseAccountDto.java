package bisq.api.dto.account.fiat;

public record WiseAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        WiseAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public WiseAccountDto {
        if (paymentRail != FiatPaymentRailDto.WISE) {
            throw new IllegalArgumentException("paymentRail must be WISE");
        }
    }

    public WiseAccountDto(String accountName,
                          WiseAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.WISE, accountPayload);
    }
}
