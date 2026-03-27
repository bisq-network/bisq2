package bisq.api.dto.account.fiat;


public record SbpAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SbpAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SbpAccountDto {
        if (paymentRail != FiatPaymentRailDto.SBP) {
            throw new IllegalArgumentException("paymentRail must be SBP");
        }
    }

    public SbpAccountDto(String accountName,
                         SbpAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SBP, accountPayload);
    }
}
