package bisq.api.dto.account.fiat;


public record MoneseAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        MoneseAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public MoneseAccountDto {
        if (paymentRail != FiatPaymentRailDto.MONESE) {
            throw new IllegalArgumentException("paymentRail must be MONESE");
        }
    }

    public MoneseAccountDto(String accountName,
                            MoneseAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.MONESE, accountPayload);
    }
}
