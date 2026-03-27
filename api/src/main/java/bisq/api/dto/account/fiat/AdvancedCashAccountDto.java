package bisq.api.dto.account.fiat;

public record AdvancedCashAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        AdvancedCashAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public AdvancedCashAccountDto {
        if (paymentRail != FiatPaymentRailDto.ADVANCED_CASH) {
            throw new IllegalArgumentException("paymentRail must be ADVANCED_CASH");
        }
    }

    public AdvancedCashAccountDto(String accountName,
                                  AdvancedCashAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.ADVANCED_CASH, accountPayload);
    }
}
