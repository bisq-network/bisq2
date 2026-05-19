package bisq.api.dto.account.fiat;

public record AchTransferAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        String holderName,
        String holderAddress,
        String bankName,
        String routingNr,
        String accountNr,
        BankAccountTypeDto bankAccountType
) implements FiatPaymentAccountPayloadDto {
}
