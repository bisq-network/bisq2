package bisq.api.dto.account.fiat;


public record ImpsAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        ImpsAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public ImpsAccountDto {
        if (paymentRail != FiatPaymentRailDto.IMPS) {
            throw new IllegalArgumentException("paymentRail must be IMPS");
        }
    }

    public ImpsAccountDto(String accountName,
                          ImpsAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.IMPS, accountPayload);
    }
}
