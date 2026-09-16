package bisq.api.dto.mappings.account.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SepaAccount;
import bisq.account.accounts.fiat.SepaAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.sepa.SepaAccountPayloadDto;
import bisq.api.dto.account.fiat.sepa.CreateSepaAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.util.StringUtils;

import java.util.List;

public class SepaAccountDtoMapping {
    public static SepaAccount toBisq2Model(String accountName, CreateSepaAccountPayloadDto payloadDto) {
        SepaAccountPayload payload = new SepaAccountPayload(
                StringUtils.createUid(),
                payloadDto.holderName(),
                payloadDto.iban(),
                payloadDto.bic(),
                payloadDto.selectedCountryCode(),
                payloadDto.acceptedCountryCodes()
        );
        return new SepaAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(SepaAccount account) {
        FiatPaymentMethodChargebackRiskDto chargebackRisk = FiatAccountDtoMappingHelper.toChargebackRiskDto(account.getPaymentMethod().getPaymentRail());
        FiatCurrencyDto currency = FiatAccountDtoMappingHelper.toFiatCurrencyDto(account.getAccountPayload().getCurrencyCode());
        CountryDto country = FiatAccountDtoMappingHelper.toCountryDto(account.getCountry().getCode());
        List<CountryDto> acceptedCountries = account.getAccountPayload().getAcceptedCountryCodes().stream()
                .map(FiatAccountDtoMappingHelper::toCountryDto)
                .toList();

        return new PaymentAccountDto(
                account.getAccountName(),
                FiatPaymentRailDto.SEPA,
                new SepaAccountPayloadDto(
                        chargebackRisk,
                        account.getPaymentMethod().getShortDisplayString(),
                        currency,
                        country,
                        acceptedCountries,
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getIban(),
                        account.getAccountPayload().getBic()
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
