package bisq.api.dto.account.fiat;


public record F2FAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        F2FAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public F2FAccountDto {
        if (paymentRail != FiatPaymentRailDto.F2F) {
            throw new IllegalArgumentException("paymentRail must be F2F");
        }
    }

    public F2FAccountDto(String accountName,
                         F2FAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.F2F, accountPayload);
    }
}
