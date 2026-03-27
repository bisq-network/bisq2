package bisq.api.dto.account.fiat;


public record PixAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        PixAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public PixAccountDto {
        if (paymentRail != FiatPaymentRailDto.PIX) {
            throw new IllegalArgumentException("paymentRail must be PIX");
        }
    }

    public PixAccountDto(String accountName,
                         PixAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.PIX, accountPayload);
    }
}
