package bisq.api.dto.account.fiat;


public record InteracETransferAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        InteracETransferAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public InteracETransferAccountDto {
        if (paymentRail != FiatPaymentRailDto.INTERAC_E_TRANSFER) {
            throw new IllegalArgumentException("paymentRail must be INTERAC_E_TRANSFER");
        }
    }

    public InteracETransferAccountDto(String accountName,
                                      InteracETransferAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.INTERAC_E_TRANSFER, accountPayload);
    }
}
