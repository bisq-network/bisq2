package bisq.api.dto.account.fiat;


public record PerfectMoneyAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        PerfectMoneyAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public PerfectMoneyAccountDto {
        if (paymentRail != FiatPaymentRailDto.PERFECT_MONEY) {
            throw new IllegalArgumentException("paymentRail must be PERFECT_MONEY");
        }
    }

    public PerfectMoneyAccountDto(String accountName,
                                  PerfectMoneyAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.PERFECT_MONEY, accountPayload);
    }
}
