package bisq.api.dto.mappings.account.fiat;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.api.dto.account.fiat.FiatPaymentMethodChargebackRiskDto;
import bisq.api.dto.account.fiat.FiatPaymentMethodDto;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.i18n.Res;
import bisq.mu_sig.MuSigTradeAmountLimits;

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

        FiatPaymentRail paymentRail = paymentMethod.getPaymentRail();
        String maxTradeLimit = MuSigTradeAmountLimits.getFormattedMaxTradeLimit(paymentRail);
        String restrictions = Res.get("paymentAccounts.summary.tradeLimit", maxTradeLimit) + " / " +
                Res.get("paymentAccounts.summary.tradeDuration", paymentRail.getTradeDuration().getDisplayString());

        return new FiatPaymentMethodDto(
                FiatPaymentRailDtoMapping.fromBisq2Model(paymentRail),
                paymentMethod.getShortDisplayString(),
                paymentMethod.getSupportedCurrencyCodesAsDisplayString(),
                countryNames,
                FiatPaymentMethodChargebackRiskDto.valueOf(paymentMethod.getPaymentRail().getChargebackRisk().name()),
                restrictions
        );
    }
}
