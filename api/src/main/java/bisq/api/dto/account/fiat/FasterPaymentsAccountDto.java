package bisq.api.dto.account.fiat;


public record FasterPaymentsAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        FasterPaymentsAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public FasterPaymentsAccountDto {
        if (paymentRail != FiatPaymentRailDto.FASTER_PAYMENTS) {
            throw new IllegalArgumentException("paymentRail must be FASTER_PAYMENTS");
        }
    }

    public FasterPaymentsAccountDto(String accountName,
                                    FasterPaymentsAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.FASTER_PAYMENTS, accountPayload);
    }
}
