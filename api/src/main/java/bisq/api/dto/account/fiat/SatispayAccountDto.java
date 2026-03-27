package bisq.api.dto.account.fiat;


public record SatispayAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SatispayAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SatispayAccountDto {
        if (paymentRail != FiatPaymentRailDto.SATISPAY) {
            throw new IllegalArgumentException("paymentRail must be SATISPAY");
        }
    }

    public SatispayAccountDto(String accountName,
                              SatispayAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SATISPAY, accountPayload);
    }
}
