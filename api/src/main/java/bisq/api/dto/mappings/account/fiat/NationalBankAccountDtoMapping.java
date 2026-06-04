package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.accounts.fiat.NationalBankAccount;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.common.BankAccountTypeDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.national_bank.CreateNationalBankAccountPayloadDto;
import bisq.api.dto.account.fiat.national_bank.NationalBankAccountPayloadDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

public class NationalBankAccountDtoMapping {
    public static NationalBankAccount toBisq2Model(String accountName, CreateNationalBankAccountPayloadDto payloadDto) {
        NationalBankAccountPayload payload = new NationalBankAccountPayload(
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
                payloadDto.nationalAccountId()
        );
        return new NationalBankAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(NationalBankAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        FiatCurrencyDto currency = FiatAccountDtoMappingHelper.toFiatCurrencyDto(account.getAccountPayload().getSelectedCurrencyCode());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.NATIONAL_BANK,
                new NationalBankAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        country,
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getHolderId(),
                        account.getAccountPayload().getBankName(),
                        account.getAccountPayload().getBankId(),
                        account.getAccountPayload().getBranchId(),
                        account.getAccountPayload().getAccountNr(),
                        account.getAccountPayload().getBankAccountType()
                                .map(bankAccountType -> BankAccountTypeDto.valueOf(bankAccountType.name())),
                        account.getAccountPayload().getNationalAccountId()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
