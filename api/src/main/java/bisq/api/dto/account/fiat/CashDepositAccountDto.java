package bisq.api.dto.account.fiat;

public record CashDepositAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        CashDepositAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public CashDepositAccountDto {
        if (paymentRail != FiatPaymentRailDto.CASH_DEPOSIT) {
            throw new IllegalArgumentException("paymentRail must be CASH_DEPOSIT");
        }
    }

    public CashDepositAccountDto(String accountName,
                                 CashDepositAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.CASH_DEPOSIT, accountPayload);
    }
}
