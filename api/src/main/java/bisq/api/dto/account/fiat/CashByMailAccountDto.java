package bisq.api.dto.account.fiat;

public record CashByMailAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        CashByMailAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public CashByMailAccountDto {
        if (paymentRail != FiatPaymentRailDto.CASH_BY_MAIL) {
            throw new IllegalArgumentException("paymentRail must be CASH_BY_MAIL");
        }
    }

    public CashByMailAccountDto(String accountName,
                                CashByMailAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.CASH_BY_MAIL, accountPayload);
    }
}
