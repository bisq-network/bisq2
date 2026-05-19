package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AchTransferAccount;
import bisq.account.accounts.fiat.AchTransferAccountPayload;
import bisq.account.accounts.fiat.BankAccountType;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.AchTransferAccountPayloadDto;
import bisq.api.dto.account.fiat.BankAccountTypeDto;
import bisq.api.dto.account.fiat.CountryDto;
import bisq.api.dto.account.fiat.FiatCurrencyDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.create.CreateAchTransferAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

public class AchTransferAccountDtoMapping {
    public static AchTransferAccount toBisq2Model(String accountName, CreateAchTransferAccountPayloadDto payloadDto) {
        AchTransferAccountPayload payload = new AchTransferAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.holderAddress(),
                payloadDto.bankName(),
                payloadDto.routingNr(),
                payloadDto.accountNr(),
                BankAccountType.valueOf(payloadDto.bankAccountType().name())
        );
        return new AchTransferAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(AchTransferAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        FiatCurrencyDto currency = FiatAccountDtoMappingHelper.toFiatCurrencyDto(account.getAccountPayload().getSelectedCurrencyCode());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.ACH_TRANSFER,
                new AchTransferAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        country,
                        account.getAccountPayload().getHolderName().orElseThrow(),
                        account.getAccountPayload().getHolderAddress(),
                        account.getAccountPayload().getBankName().orElseThrow(),
                        account.getAccountPayload().getBankId().orElseThrow(),
                        account.getAccountPayload().getAccountNr(),
                        BankAccountTypeDto.valueOf(account.getAccountPayload().getBankAccountType().orElseThrow().name())
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
