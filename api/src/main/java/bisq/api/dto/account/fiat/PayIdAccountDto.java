package bisq.api.dto.account.fiat;


public record PayIdAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        PayIdAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public PayIdAccountDto {
        if (paymentRail != FiatPaymentRailDto.PAY_ID) {
            throw new IllegalArgumentException("paymentRail must be PAY_ID");
        }
    }

    public PayIdAccountDto(String accountName,
                           PayIdAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.PAY_ID, accountPayload);
    }
}
