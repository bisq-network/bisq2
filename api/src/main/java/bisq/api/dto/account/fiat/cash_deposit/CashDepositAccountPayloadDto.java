package bisq.api.dto.account.fiat.cash_deposit;

import bisq.api.dto.account.fiat.common.BankAccountTypeDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

import java.util.Optional;

public record CashDepositAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        String holderName,
        Optional<String> holderId,
        String bankName,
        Optional<String> bankId,
        Optional<String> branchId,
        String accountNr,
        Optional<BankAccountTypeDto> bankAccountType,
        Optional<String> nationalAccountId,
        Optional<String> requirements
) implements FiatPaymentAccountPayloadDto {
}
