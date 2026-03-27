package bisq.api.dto.account.fiat;

public record AliPayAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        AliPayAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public AliPayAccountDto {
        if (paymentRail != FiatPaymentRailDto.ALI_PAY) {
            throw new IllegalArgumentException("paymentRail must be ALI_PAY");
        }
    }

    public AliPayAccountDto(String accountName,
                            AliPayAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.ALI_PAY, accountPayload);
    }
}
