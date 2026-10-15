package bisq.api.dto.account.fiat.national_bank;

import bisq.api.dto.account.fiat.common.BankAccountTypeDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;

import java.util.Optional;

public record NationalBankAccountPayloadDto(
        FiatPaymentMethodChargebackRiskDto chargebackRisk,
        String paymentMethodName,
        FiatCurrencyDto currency,
        CountryDto country,
        Optional<String> holderName,
        Optional<String> holderId,
        Optional<String> bankName,
        Optional<String> bankId,
        Optional<String> branchId,
        String accountNr,
        Optional<BankAccountTypeDto> bankAccountType,
        Optional<String> nationalAccountId
) implements FiatPaymentAccountPayloadDto {
}
