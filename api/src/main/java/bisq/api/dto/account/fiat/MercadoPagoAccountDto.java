package bisq.api.dto.account.fiat;


public record MercadoPagoAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        MercadoPagoAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public MercadoPagoAccountDto {
        if (paymentRail != FiatPaymentRailDto.MERCADO_PAGO) {
            throw new IllegalArgumentException("paymentRail must be MERCADO_PAGO");
        }
    }

    public MercadoPagoAccountDto(String accountName,
                                 MercadoPagoAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.MERCADO_PAGO, accountPayload);
    }
}
