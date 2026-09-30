package bisq.api.dto.account.fiat.ach;

import bisq.api.dto.account.fiat.common.BankAccountTypeDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

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
