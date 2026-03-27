package bisq.api.dto.account.fiat;


public record MoneyGramAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        MoneyGramAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public MoneyGramAccountDto {
        if (paymentRail != FiatPaymentRailDto.MONEY_GRAM) {
            throw new IllegalArgumentException("paymentRail must be MONEY_GRAM");
        }
    }

    public MoneyGramAccountDto(String accountName,
                               MoneyGramAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.MONEY_GRAM, accountPayload);
    }
}
