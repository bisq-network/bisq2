package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.MoneyGramAccount;
import bisq.account.accounts.fiat.MoneyGramAccountPayload;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.money_gram.MoneyGramAccountPayloadDto;
import bisq.api.dto.account.fiat.money_gram.CreateMoneyGramAccountPayloadDto;
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
