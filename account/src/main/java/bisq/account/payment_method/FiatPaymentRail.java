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

package bisq.account.payment_method;

import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The payment rails for fiat payments.
 * Provide static data associated with the payment rail.
 */
public enum FiatPaymentRail implements PaymentRail {
    CUSTOM(new ArrayList<>(), new ArrayList<>()),             // Custom defined payment rail by the user
    SEPA(FiatPaymentRailUtil.getSepaEuroCountries()),
    SEPA_INSTANT(FiatPaymentRailUtil.getSepaEuroCountries()),
    ZELLE(List.of("US")),
    REVOLUT(FiatPaymentRailUtil.getRevolutCountries(), FiatPaymentRailUtil.getRevolutCurrencies()),
    WISE(FiatPaymentRailUtil.getWiseCountries(), FiatPaymentRailUtil.getWiseCurrencies()),
    NATIONAL_BANK(new ArrayList<>(), new ArrayList<>()),
    SWIFT(),
    F2F(),
    ACH_TRANSFER(List.of("US"), List.of("USD")),
    PIX(List.of("BR"), List.of("BRL")),
    FASTER_PAYMENTS(List.of("GB"), List.of("GBP")),
    PAY_ID(List.of("AU"), List.of("AUD")),
    US_POSTAL_MONEY_ORDER(List.of("US"), List.of("USD")),
    CASH_BY_MAIL(),
    STRIKE(List.of("US", "SV"), List.of("USD")),
    INTERAC_E_TRANSFER(new ArrayList<>(), List.of("CAD")),
    AMAZON_GIFT_CARD(
            new ArrayList<>(),
            List.of("AUD", "CAD", "EUR", "GBP", "INR", "JPY", "SAR", "SEK", "SGD", "TRY", "USD")),
    CASH_DEPOSIT(),
    UPI(new ArrayList<>(), List.of("INR")),
    BIZUM(List.of("ES"), List.of("EUR")),
    CASH_APP(List.of("US"), List.of("USD"));

    @Getter
    @EqualsAndHashCode.Exclude
    private final List<Country> countries;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<TradeCurrency> tradeCurrencies;
    @Getter
    @EqualsAndHashCode.Exclude
    private final List<String> currencyCodes;
    @EqualsAndHashCode.Exclude
    private final Set<String> currencyCodesAsSet;

    FiatPaymentRail() {
        this(null, null);
    }

    FiatPaymentRail(List<String> countryCodes) {
        this(countryCodes, null);
    }

    /**
     * @param countryCodes  If countryCodes is null we use all countries
     * @param currencyCodes If currencyCodes is  null we create it from the countries
     */
    FiatPaymentRail(@Nullable List<String> countryCodes, @Nullable List<String> currencyCodes) {
        countries = countryCodes != null ?
                CountryRepository.getCountriesFromCodes(countryCodes) :
                CountryRepository.getCountries();
        countries.sort(Comparator.comparing(Country::getName));

        this.tradeCurrencies = currencyCodes != null ?
                FiatPaymentRailUtil.toTradeCurrencies(currencyCodes) :
                countries.stream()
                        .map(country -> FiatCurrencyRepository.getCurrencyByCountryCode(country.getCode()))
                        .distinct()
                        .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                        .collect(Collectors.toList());
        this.tradeCurrencies.sort(Comparator.comparing(TradeCurrency::getDisplayName));

        this.currencyCodes = currencyCodes != null ?
                currencyCodes :
                tradeCurrencies.stream().map(TradeCurrency::getCode).collect(Collectors.toList());

        currencyCodesAsSet = new HashSet<>(this.currencyCodes);
        // sorting this.currencyCodes throws an ExceptionInInitializerError. Not clear why. 
        // But currencyCodes comes from hard coded values, so it is deterministic anyway.
    }

    public boolean supportsCurrency(String currencyCode) {
        return currencyCodesAsSet.contains(currencyCode);
    }
}

    /*
Most important methods are added already. we can add more on demand/request later:
Here are the missing ones (not updated with maybe newly added ones in Bisq1)
SAME_BANK=Transfer with same bank
SPECIFIC_BANKS=Transfers with specific banks
MONEY_GRAM=MoneyGram
WESTERN_UNION=Western Union
JAPAN_BANK=Japan Bank Furikomi
UPHOLD=Uphold
MONEY_BEAM=MoneyBeam (N26)
POPMONEY=Popmoney
PERFECT_MONEY=Perfect Money
ALI_PAY=AliPay
WECHAT_PAY=WeChat Pay
SEPA_INSTANT=SEPA Instant Payments
SWISH=Swish
CHASE_QUICK_PAY=Chase QuickPay
HAL_CASH=HalCash
PROMPT_PAY=PromptPay
ADVANCED_CASH=Advanced Cash
WISE_USD=Wise-USD
PAYSERA=Paysera
PAXUM=Paxum
NEFT=India/NEFT
RTGS=India/RTGS
IMPS=India/IMPS
PAYTM=India/PayTM
NEQUI=Nequi
CAPITUAL=Capitual
CELPAY=CelPay
MONESE=Monese
SATISPAY=Satispay
TIKKIE=Tikkie
VERSE=Verse
SWIFT=SWIFT International Wire Transfer
DOMESTIC_WIRE_TRANSFER=Domestic Wire Transfer
CIPS=Cross-Border Interbank Payment System
*/