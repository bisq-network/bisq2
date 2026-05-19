package bisq.api.dto.account.fiat;

public record AliPayAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        String accountNr
) implements FiatPaymentAccountPayloadDto {
}
