package bisq.api.dto.account.fiat;


public record SwishAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SwishAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SwishAccountDto {
        if (paymentRail != FiatPaymentRailDto.SWISH) {
            throw new IllegalArgumentException("paymentRail must be SWISH");
        }
    }

    public SwishAccountDto(String accountName,
                           SwishAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SWISH, accountPayload);
    }
}
