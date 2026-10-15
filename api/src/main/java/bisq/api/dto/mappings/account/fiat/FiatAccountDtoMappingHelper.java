package bisq.api.dto.mappings.account.fiat;

import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.api.dto.account.fiat.common.CountryDto;
import bisq.api.dto.account.fiat.common.FiatCurrencyDto;
import bisq.api.dto.account.fiat.payment_method.FiatPaymentMethodChargebackRiskDto;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;

import java.util.List;

public final class FiatAccountDtoMappingHelper {
    private FiatAccountDtoMappingHelper() {
    }

    public static FiatPaymentMethodChargebackRiskDto toChargebackRiskDto(FiatPaymentRail paymentRail) {
        return FiatPaymentMethodChargebackRiskDto.valueOf(paymentRail.getChargebackRisk().name());
    }

    public static FiatCurrencyDto toFiatCurrencyDto(String currencyCode) {
        return new FiatCurrencyDto(
                currencyCode,
                FiatCurrencyRepository.getCurrencyByCode(currencyCode).getDisplayName()
        );
    }

    public static List<FiatCurrencyDto> toFiatCurrencyDtos(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatAccountDtoMappingHelper::toFiatCurrencyDto)
                .toList();
    }

    public static CountryDto toCountryDto(String countryCode) {
        return new CountryDto(
                countryCode,
                CountryRepository.getNameByCode(countryCode)
        );
    }
}
