package bisq.api.dto.account.fiat;


public record HalCashAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        HalCashAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public HalCashAccountDto {
        if (paymentRail != FiatPaymentRailDto.HAL_CASH) {
            throw new IllegalArgumentException("paymentRail must be HAL_CASH");
        }
    }

    public HalCashAccountDto(String accountName,
                             HalCashAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.HAL_CASH, accountPayload);
    }
}
