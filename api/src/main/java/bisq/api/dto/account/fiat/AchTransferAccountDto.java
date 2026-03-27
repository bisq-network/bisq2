package bisq.api.dto.account.fiat;

public record AchTransferAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        AchTransferAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public AchTransferAccountDto {
        if (paymentRail != FiatPaymentRailDto.ACH_TRANSFER) {
            throw new IllegalArgumentException("paymentRail must be ACH_TRANSFER");
        }
    }

    public AchTransferAccountDto(String accountName,
                                 AchTransferAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.ACH_TRANSFER, accountPayload);
    }
}


