package bisq.api.dto.account.fiat;

public record WeChatPayAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        WeChatPayAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public WeChatPayAccountDto {
        if (paymentRail != FiatPaymentRailDto.WECHAT_PAY) {
            throw new IllegalArgumentException("paymentRail must be WECHAT_PAY");
        }
    }

    public WeChatPayAccountDto(String accountName,
                               WeChatPayAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.WECHAT_PAY, accountPayload);
    }
}
