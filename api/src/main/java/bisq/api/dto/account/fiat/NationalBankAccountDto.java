package bisq.api.dto.account.fiat;


public record NationalBankAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        NationalBankAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public NationalBankAccountDto {
        if (paymentRail != FiatPaymentRailDto.NATIONAL_BANK) {
            throw new IllegalArgumentException("paymentRail must be NATIONAL_BANK");
        }
    }

    public NationalBankAccountDto(String accountName,
                                  NationalBankAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.NATIONAL_BANK, accountPayload);
    }
}
