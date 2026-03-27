package bisq.api.dto.account.fiat;


public record StrikeAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        StrikeAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public StrikeAccountDto {
        if (paymentRail != FiatPaymentRailDto.STRIKE) {
            throw new IllegalArgumentException("paymentRail must be STRIKE");
        }
    }

    public StrikeAccountDto(String accountName,
                            StrikeAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.STRIKE, accountPayload);
    }
}
