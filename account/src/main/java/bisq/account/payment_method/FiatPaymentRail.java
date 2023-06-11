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

public enum FiatPaymentRail implements PaymentRail {
    CUSTOM(new ArrayList<>(), new ArrayList<>()),
    SEPA(FiatPaymentRailUtil.getSepaEuroCountries()),
    SEPA_INSTANT(FiatPaymentRailUtil.getSepaEuroCountries()),
    ZELLE(List.of("US")),
    REVOLUT(FiatPaymentRailUtil.getRevolutCountries(), FiatPaymentRailUtil.getRevolutCurrencies()),
    WISE(FiatPaymentRailUtil.getWiseCountries(), FiatPaymentRailUtil.getWiseCurrencies()),
    NATIONAL_BANK(new ArrayList<>(), new ArrayList<>()),
    SWIFT();

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
        this.tradeCurrencies.sort(Comparator.comparing(TradeCurrency::getName));

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
TODO add missing bisq 1 payment methods with supported countries and currencies
NATIONAL_BANK=National bank transfer
SAME_BANK=Transfer with same bank
SPECIFIC_BANKS=Transfers with specific banks
US_POSTAL_MONEY_ORDER=US Postal Money Order
CASH_DEPOSIT=Cash Deposit
CASH_BY_MAIL=Cash By Mail
MONEY_GRAM=MoneyGram
WESTERN_UNION=Western Union
F2F=Face to face (in person)
JAPAN_BANK=Japan Bank Furikomi
AUSTRALIA_PAY_ID=Australian PayID

UPHOLD=Uphold
MONEY_BEAM=MoneyBeam (N26)
POPMONEY=Popmoney
REVOLUT=Revolut
PERFECT_MONEY=Perfect Money
ALI_PAY=AliPay
WECHAT_PAY=WeChat Pay
SEPA=SEPA
SEPA_INSTANT=SEPA Instant Payments
FASTER_PAYMENTS=Faster Payments
SWISH=Swish
ZELLE=Zelle
CHASE_QUICK_PAY=Chase QuickPay
INTERAC_E_TRANSFER=Interac e-Transfer
HAL_CASH=HalCash
PROMPT_PAY=PromptPay
ADVANCED_CASH=Advanced Cash
WISE=Wise
WISE_USD=Wise-USD
PAYSERA=Paysera
PAXUM=Paxum
NEFT=India/NEFT
RTGS=India/RTGS
IMPS=India/IMPS
UPI=India/UPI
PAYTM=India/PayTM
NEQUI=Nequi
BIZUM=Bizum
PIX=Pix
AMAZON_GIFT_CARD=Amazon eGift Card
CAPITUAL=Capitual
CELPAY=CelPay
MONESE=Monese
SATISPAY=Satispay
TIKKIE=Tikkie
VERSE=Verse
STRIKE=Strike
SWIFT=SWIFT International Wire Transfer
ACH_TRANSFER=ACH Transfer
DOMESTIC_WIRE_TRANSFER=Domestic Wire Transfer
CIPS=Cross-Border Interbank Payment System
*/