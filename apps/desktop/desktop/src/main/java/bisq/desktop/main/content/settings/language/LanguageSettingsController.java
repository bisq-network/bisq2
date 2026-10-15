/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.settings.language;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.asset.FiatCurrencyRepository;
import bisq.common.locale.CountryRepository;
import bisq.common.locale.LanguageRepository;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.settings.SettingsService;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static bisq.common.locale.CountryRepository.getLocalizedCountryDisplayString;

@Slf4j
public class LanguageSettingsController implements Controller {
    @Getter
    private final LanguageSettingsView view;
    private final LanguageSettingsModel model;
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private Pin supportedLanguageTagsPin;

    public LanguageSettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();

        List<String> allCountyCodes = CountryRepository.getAllCountyCodes().stream()
                .sorted((c1, c2) ->
                        getLocalizedCountryDisplayString(c1).compareToIgnoreCase(getLocalizedCountryDisplayString(c2)))
                .toList();
        List<String> currencyCodes = MarketRepository.getAllFiatMarkets().stream()
                .map(Market::getQuoteCurrencyCode)
                .distinct()
                .sorted()
                .toList();
        StringConverter<String> languageStringConverter = new StringConverter<>() {
            @Override
            public String toString(String languageCode) {
                return languageCode != null ? LanguageRepository.getDisplayString(languageCode) : "";
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        };
        StringConverter<String> countryStringConverter = new StringConverter<>() {
            @Override
            public String toString(String countryCode) {
                return countryCode != null ? getLocalizedCountryDisplayString(countryCode) : "";
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        };
        StringConverter<String> currencyStringConverter = new StringConverter<>() {
            @Override
            public String toString(String currencyCode) {
                return currencyCode != null
                        ? FiatCurrencyRepository.getDisplayNameAndCode(currencyCode)
                        : "";
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        };

        model = new LanguageSettingsModel(
                settingsService.getLanguageTag().get(),
                settingsService.getCountryCode().get(),
                settingsService.getCurrencyCode().get(),
                allCountyCodes,
                currencyCodes,
                languageStringConverter,
                countryStringConverter,
                currencyStringConverter
        );

        view = new LanguageSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        model.getLanguageTags().setAll(getSortedLanguageTags());
        model.getSupportedLanguageTags().setAll(getSortedLanguageTags());

        supportedLanguageTagsPin = FxBindings.<String, String>bind(model.getSelectedSupportedLanguageTags())
                .to(settingsService.getSupportedLanguageTags());
        model.getSupportedLanguageTagsFilteredList().setPredicate(e -> !model.getSelectedSupportedLanguageTags().contains(e));
    }

    @Override
    public void onDeactivate() {
        supportedLanguageTagsPin.unbind();
    }


    // UI handlers
    void onSelectCountryCode(@Nullable String countryCode) {
        if (countryCode == null) {
            return;
        }
        model.setSelectedCountryCode(countryCode);
        settingsService.setCountryCode(countryCode);
        try {
            Locale locale = new Locale.Builder().setRegion(countryCode).build();
            Currency defaultCurrency = Currency.getInstance(locale);
            if (defaultCurrency != null) {
                selectCurrencyCode(defaultCurrency.getCurrencyCode());
            }
        } catch (Exception e) {
            log.warn("Could not derive default currency for country: {}", countryCode);
        }
    }

    void onSelectCurrencyCode(@Nullable String currencyCode) {
        selectCurrencyCode(currencyCode);
    }

    void onSelectLanguageTag(@Nullable String languageTag) {
        if (languageTag == null) {
            return;
        }
        model.getSelectedLanguageTag().set(languageTag);
        settingsService.setLanguageTag(languageTag);
    }

    void onSelectSupportedLanguage(@Nullable String languageTag) {
        if (languageTag != null) model.getSelectedLSupportedLanguageTag().set(languageTag);
    }

    void onAddSupportedLanguage() {
        String selected = model.getSelectedLSupportedLanguageTag().get();
        if (StringUtils.isNotEmpty(selected)) {
            settingsService.getSupportedLanguageTags().add(selected);
            model.getSelectedLSupportedLanguageTag().set(null);
        }
    }

    void onRemoveSupportedLanguage(@Nullable String languageTag) {
        if (StringUtils.isNotEmpty(languageTag)) settingsService.getSupportedLanguageTags().remove(languageTag);
    }


    // Private
    private void selectCurrencyCode(@Nullable String currencyCode) {
        if (currencyCode == null) {
            return;
        }
        MarketRepository.getAllFiatMarkets().stream()
                .filter(m -> m.getQuoteCurrencyCode().equalsIgnoreCase(currencyCode))
                .findFirst()
                .ifPresent(market -> {
                    model.setSelectedCurrencyCode(currencyCode);
                    settingsService.setCurrencyCode(currencyCode);
                    marketPriceService.setSelectedMarket(market);
                });
    }

    private List<String> getSortedLanguageTags() {
        return LanguageRepository.LANGUAGE_TAGS.stream()
                .sorted(Comparator.comparing(LanguageRepository::getDisplayString))
                .toList();
    }
}
