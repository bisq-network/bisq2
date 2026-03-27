package bisq.api.dto.account.fiat;


public record SepaInstantAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SepaInstantAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SepaInstantAccountDto {
        if (paymentRail != FiatPaymentRailDto.SEPA_INSTANT) {
            throw new IllegalArgumentException("paymentRail must be SEPA_INSTANT");
        }
    }

    public SepaInstantAccountDto(String accountName,
                                 SepaInstantAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SEPA_INSTANT, accountPayload);
    }
}
