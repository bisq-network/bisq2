package bisq.api.dto.account.fiat;


public record PayseraAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        PayseraAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public PayseraAccountDto {
        if (paymentRail != FiatPaymentRailDto.PAYSERA) {
            throw new IllegalArgumentException("paymentRail must be PAYSERA");
        }
    }

    public PayseraAccountDto(String accountName,
                             PayseraAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.PAYSERA, accountPayload);
    }
}
