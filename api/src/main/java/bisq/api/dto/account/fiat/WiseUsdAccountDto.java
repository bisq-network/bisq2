package bisq.api.dto.account.fiat;

public record WiseUsdAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        WiseUsdAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public WiseUsdAccountDto {
        if (paymentRail != FiatPaymentRailDto.WISE_USD) {
            throw new IllegalArgumentException("paymentRail must be WISE_USD");
        }
    }

    public WiseUsdAccountDto(String accountName,
                             WiseUsdAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.WISE_USD, accountPayload);
    }
}
