package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.accounts.fiat.CashDepositAccount;
import bisq.account.accounts.fiat.CashDepositAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.common.BankAccountTypeDto;
import bisq.api.dto.account.fiat.cash_deposit.CashDepositAccountPayloadDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.cash_deposit.CreateCashDepositAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

public class CashDepositAccountDtoMapping {
    public static CashDepositAccount toBisq2Model(String accountName, CreateCashDepositAccountPayloadDto payloadDto) {
        CashDepositAccountPayload payload = new CashDepositAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCountryCode(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.holderName(),
                payloadDto.holderId(),
                payloadDto.bankName(),
                payloadDto.bankId(),
                payloadDto.branchId(),
                payloadDto.accountNr(),
                payloadDto.bankAccountType().map(bankAccountType -> BankAccountType.valueOf(bankAccountType.name())),
                payloadDto.nationalAccountId(),
                payloadDto.requirements()
        );
        return new CashDepositAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(CashDepositAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        FiatCurrencyDto currency = FiatAccountDtoMappingHelper.toFiatCurrencyDto(account.getAccountPayload().getSelectedCurrencyCode());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.CASH_DEPOSIT,
                new CashDepositAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        country,
                        account.getAccountPayload().getHolderName().orElse(null),
                        account.getAccountPayload().getHolderId(),
                        account.getAccountPayload().getBankName().orElse(null),
                        account.getAccountPayload().getBankId(),
                        account.getAccountPayload().getBranchId(),
                        account.getAccountPayload().getAccountNr(),
                        account.getAccountPayload().getBankAccountType()
                                .map(bankAccountType -> BankAccountTypeDto.valueOf(bankAccountType.name())),
                        account.getAccountPayload().getNationalAccountId(),
                        account.getAccountPayload().getRequirements()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
