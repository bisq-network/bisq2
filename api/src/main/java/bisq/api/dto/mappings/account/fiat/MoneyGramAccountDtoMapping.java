package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.MoneyGramAccount;
import bisq.account.accounts.fiat.MoneyGramAccountPayload;
import bisq.api.dto.account.fiat.CountryDto;
import bisq.api.dto.account.fiat.FiatCurrencyDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.FiatPaymentRailDto;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.MoneyGramAccountPayloadDto;
import bisq.api.dto.account.fiat.create.CreateMoneyGramAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

import java.util.List;

public class MoneyGramAccountDtoMapping {
    public static MoneyGramAccount toBisq2Model(String accountName, CreateMoneyGramAccountPayloadDto payloadDto) {
        MoneyGramAccountPayload payload = new MoneyGramAccountPayload(
                StringUtils.createUid(),
                payloadDto.selectedCountryCode(),
                payloadDto.selectedCurrencyCodes(),
                payloadDto.holderName(),
                payloadDto.email(),
                payloadDto.state()
        );
        return new MoneyGramAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(MoneyGramAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());
        List<FiatCurrencyDto> selectedCurrencies = FiatAccountDtoMappingHelper.toFiatCurrencyDtos(account.getAccountPayload().getSelectedCurrencyCodes());

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.MONEY_GRAM,
                new MoneyGramAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        country,
                        selectedCurrencies,
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getEmail(),
                        account.getAccountPayload().getState()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
