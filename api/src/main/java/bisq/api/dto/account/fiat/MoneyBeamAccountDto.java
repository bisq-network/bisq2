package bisq.api.dto.account.fiat;


public record MoneyBeamAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        MoneyBeamAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public MoneyBeamAccountDto {
        if (paymentRail != FiatPaymentRailDto.MONEY_BEAM) {
            throw new IllegalArgumentException("paymentRail must be MONEY_BEAM");
        }
    }

    public MoneyBeamAccountDto(String accountName,
                               MoneyBeamAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.MONEY_BEAM, accountPayload);
    }
}
