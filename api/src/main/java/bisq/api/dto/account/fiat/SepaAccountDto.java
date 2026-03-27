package bisq.api.dto.account.fiat;


public record SepaAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SepaAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SepaAccountDto {
        if (paymentRail != FiatPaymentRailDto.SEPA) {
            throw new IllegalArgumentException("paymentRail must be SEPA");
        }
    }

    public SepaAccountDto(String accountName,
                          SepaAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SEPA, accountPayload);
    }
}
