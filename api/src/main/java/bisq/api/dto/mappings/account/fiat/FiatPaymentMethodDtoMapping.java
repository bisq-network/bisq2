package bisq.api.dto.mappings.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodDto;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.i18n.Res;

import java.util.List;
import java.util.stream.Collectors;

public class FiatPaymentMethodDtoMapping {
    public static FiatPaymentMethodDto fromBisq2Model(FiatPaymentMethod paymentMethod) {
        List<Country> supportedCountries = paymentMethod.getSupportedCountries();
        List<String> countryCodes = supportedCountries.stream()
                .map(Country::getCode)
                .sorted()
                .toList();
        String countryNames = CountryRepository.matchesAllCountries(countryCodes)
                ? Res.get("paymentAccounts.allCountries")
                : supportedCountries.stream()
                .map(Country::getName)
                .sorted()
                .collect(Collectors.joining(", "));

        return new FiatPaymentMethodDto(
                FiatPaymentRailDtoMapping.fromBisq2Model(paymentMethod.getPaymentRail()),
                paymentMethod.getShortDisplayString(),
                paymentMethod.getSupportedCurrencyCodesAsDisplayString(),
                paymentMethod.getSupportedCurrencyDisplayNameAndCodeAsDisplayString(),
                countryNames,
                FiatPaymentMethodChargebackRiskDto.valueOf(paymentMethod.getPaymentRail().getChargebackRisk().name())
        );
    }
}
