package bisq.api.dto.account.fiat;


public record NeftAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        NeftAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public NeftAccountDto {
        if (paymentRail != FiatPaymentRailDto.NEFT) {
            throw new IllegalArgumentException("paymentRail must be NEFT");
        }
    }

    public NeftAccountDto(String accountName,
                          NeftAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.NEFT, accountPayload);
    }
}
