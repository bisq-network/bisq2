package bisq.api.dto.account.fiat;


public record Pin4AccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        Pin4AccountPayloadDto accountPayload
) implements FiatAccountDto {

    public Pin4AccountDto {
        if (paymentRail != FiatPaymentRailDto.PIN_4) {
            throw new IllegalArgumentException("paymentRail must be PIN_4");
        }
    }

    public Pin4AccountDto(String accountName,
                          Pin4AccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.PIN_4, accountPayload);
    }
}
