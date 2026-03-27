package bisq.api.dto.account.fiat;


public record SameBankAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SameBankAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SameBankAccountDto {
        if (paymentRail != FiatPaymentRailDto.SAME_BANK) {
            throw new IllegalArgumentException("paymentRail must be SAME_BANK");
        }
    }

    public SameBankAccountDto(String accountName,
                              SameBankAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SAME_BANK, accountPayload);
    }
}
