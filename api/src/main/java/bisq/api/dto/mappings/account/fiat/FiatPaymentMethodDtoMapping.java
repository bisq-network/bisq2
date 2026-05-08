package bisq.api.dto.mappings.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodDto;
import bisq.api.dto.mappings.account.PaymentMethodDtoHelper;
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
                : countryCodes.stream()
                .map(CountryRepository::getLocalizedCountryDisplayString)
                .sorted()
                .collect(Collectors.joining(", "));

<<<<<<< HEAD
=======
        FiatPaymentRail paymentRail = paymentMethod.getPaymentRail();

>>>>>>> b9fa178e25 (Update musig payment method and account models for api app)
        return new FiatPaymentMethodDto(
                FiatPaymentRailDtoMapping.fromBisq2Model(paymentMethod.getPaymentRail()),
                paymentMethod.getShortDisplayString(),
                paymentMethod.getSupportedCurrencyCodesAsDisplayString(),
                countryNames,
<<<<<<< HEAD
                FiatPaymentMethodChargebackRiskDto.valueOf(paymentMethod.getPaymentRail().getChargebackRisk().name())
=======
                FiatPaymentMethodChargebackRiskDto.valueOf(paymentMethod.getPaymentRail().getChargebackRisk().name()),
                PaymentMethodDtoHelper.getTradeLimitInfo(paymentRail),
                PaymentMethodDtoHelper.getTradeDuration(paymentRail)
>>>>>>> b9fa178e25 (Update musig payment method and account models for api app)
        );
    }
}
