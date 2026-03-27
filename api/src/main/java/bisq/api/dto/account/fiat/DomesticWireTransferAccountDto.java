package bisq.api.dto.account.fiat;

public record DomesticWireTransferAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        DomesticWireTransferAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public DomesticWireTransferAccountDto {
        if (paymentRail != FiatPaymentRailDto.DOMESTIC_WIRE_TRANSFER) {
            throw new IllegalArgumentException("paymentRail must be DOMESTIC_WIRE_TRANSFER");
        }
    }

    public DomesticWireTransferAccountDto(String accountName,
                                          DomesticWireTransferAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.DOMESTIC_WIRE_TRANSFER, accountPayload);
    }
}
