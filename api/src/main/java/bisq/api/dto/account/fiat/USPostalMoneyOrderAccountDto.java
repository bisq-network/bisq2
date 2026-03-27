package bisq.api.dto.account.fiat;

public record USPostalMoneyOrderAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        USPostalMoneyOrderAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public USPostalMoneyOrderAccountDto {
        if (paymentRail != FiatPaymentRailDto.US_POSTAL_MONEY_ORDER) {
            throw new IllegalArgumentException("paymentRail must be US_POSTAL_MONEY_ORDER");
        }
    }

    public USPostalMoneyOrderAccountDto(String accountName,
                                        USPostalMoneyOrderAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.US_POSTAL_MONEY_ORDER, accountPayload);
    }
}
