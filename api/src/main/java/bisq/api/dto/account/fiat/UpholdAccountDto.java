package bisq.api.dto.account.fiat;


public record UpholdAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        UpholdAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public UpholdAccountDto {
        if (paymentRail != FiatPaymentRailDto.UPHOLD) {
            throw new IllegalArgumentException("paymentRail must be UPHOLD");
        }
    }

    public UpholdAccountDto(String accountName,
                            UpholdAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.UPHOLD, accountPayload);
    }
}
