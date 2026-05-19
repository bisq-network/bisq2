package bisq.api.dto.mappings.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodDto;
import bisq.api.dto.mappings.account.PaymentMethodDtoMappingHelper;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;

import java.util.Comparator;
import java.util.List;

public class FiatPaymentMethodDtoMapping {
    public static FiatPaymentMethodDto fromBisq2Model(FiatPaymentMethod paymentMethod) {
        List<Country> supportedCountries = paymentMethod.getSupportedCountries();
        List<String> countryCodes = supportedCountries.stream()
                .map(Country::getCode)
                .sorted()
                .toList();
        boolean matchesAllCountries = CountryRepository.matchesAllCountries(countryCodes);

        List<CountryDto> supportedCountryDtos = supportedCountries.stream()
                .map(country -> new CountryDto(
                        country.getCode(),
                        CountryRepository.getLocalizedCountryDisplayString(country.getCode())
                ))
                .sorted(Comparator.comparing(CountryDto::name).thenComparing(CountryDto::code))
                .toList();

        List<FiatCurrencyDto> supportedCurrencies = paymentMethod.getSupportedCurrencies().stream()
                .map(asset -> new FiatCurrencyDto(asset.getCode(), asset.getDisplayName()))
                .sorted(Comparator.comparing(FiatCurrencyDto::code))
                .toList();

        FiatPaymentRail paymentRail = paymentMethod.getPaymentRail();

        return new FiatPaymentMethodDto(
                FiatPaymentRailDtoMapping.fromBisq2Model(paymentRail),
                paymentMethod.getShortDisplayString(),
                supportedCurrencies,
                supportedCountryDtos,
                matchesAllCountries,
                FiatPaymentMethodChargebackRiskDto.valueOf(paymentMethod.getPaymentRail().getChargebackRisk().name()),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(paymentRail),
                PaymentMethodDtoMappingHelper.getTradeDuration(paymentRail)
        );
    }
}
