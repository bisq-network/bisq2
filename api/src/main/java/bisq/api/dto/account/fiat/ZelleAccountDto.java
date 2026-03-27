package bisq.api.dto.account.fiat;

public record ZelleAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        ZelleAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public ZelleAccountDto {
        if (paymentRail != FiatPaymentRailDto.ZELLE) {
            throw new IllegalArgumentException("paymentRail must be ZELLE");
        }
    }

    public ZelleAccountDto(String accountName,
                           ZelleAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.ZELLE, accountPayload);
    }
}
