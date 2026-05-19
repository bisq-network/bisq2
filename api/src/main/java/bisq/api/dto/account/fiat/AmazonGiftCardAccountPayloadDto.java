package bisq.api.dto.account.fiat;

public record AmazonGiftCardAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        String emailOrMobileNr
) implements FiatPaymentAccountPayloadDto {
}
