package bisq.api.dto.account.fiat;

public record BizumAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        BizumAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public BizumAccountDto {
        if (paymentRail != FiatPaymentRailDto.BIZUM) {
            throw new IllegalArgumentException("paymentRail must be BIZUM");
        }
    }

    public BizumAccountDto(String accountName,
                           BizumAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.BIZUM, accountPayload);
    }
}
