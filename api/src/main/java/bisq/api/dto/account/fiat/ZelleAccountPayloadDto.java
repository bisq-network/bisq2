package bisq.api.dto.account.fiat;

public record ZelleAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        String currency,
        String country,
        String holderName,
        String emailOrMobileNr
) implements FiatPaymentAccountPayloadDto {
}
