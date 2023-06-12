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

package bisq.account.payment;

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@EqualsAndHashCode(callSuper = true)
public class FiatPayment extends Payment<FiatPayment.Method> {

    public static List<Method> getPaymentMethods() {
        Method[] values = Method.values();
        return List.of(values);
    }

    public static FiatPayment fromName(String paymentMethodName) {
        try {
            return new FiatPayment(FiatPayment.Method.valueOf(paymentMethodName));
        } catch (IllegalArgumentException e) {
            return new FiatPayment(paymentMethodName);
        }
    }

    public static List<FiatPayment.Method> getPaymentMethodsForProtocolType(ProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
            case BISQ_MULTISIG:
                return FiatPayment.getPaymentMethods();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
            case LIGHTNING_X:
                throw new IllegalArgumentException("No paymentMethods for that protocolType");
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }

    public static List<String> getPaymentMethodEnumNamesForCode(String currencyCode) {
        return getPaymentMethodsForCode(currencyCode).stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static List<FiatPayment.Method> getPaymentMethodsForCode(String currencyCode) {
        return FiatPayment.getPaymentMethods().stream()
                .filter(method -> {
                    if (currencyCode.equals("EUR") && (method == Method.SWIFT || method == Method.NATIONAL_BANK)) {
                        // For EUR, we don't add SWIFT and NATIONAL_BANK
                        return false;
                    }
                    // We add NATIONAL_BANK to all
                    if (method == Method.NATIONAL_BANK) {
                        return true;
                    }
                    return new HashSet<>(method.getCurrencyCodes()).contains(currencyCode);
                })
                .collect(Collectors.toList());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Payment.Method {
        USER_DEFINED(new ArrayList<>(), new ArrayList<>()),
        SEPA(getSepaEuroCountries()),
        SEPA_INSTANT(getSepaEuroCountries()),
        ZELLE(List.of("US")),
        REVOLUT(getRevolutCountries(), getRevolutCurrencies()),
        WISE(getWiseCountries(), getWiseCurrencies()),
        NATIONAL_BANK(new ArrayList<>(), new ArrayList<>()),
        SWIFT();

        @Getter
        private final List<Country> countries;
        @Getter
        private final List<TradeCurrency> tradeCurrencies;
        @Getter
        private final List<String> currencyCodes;

        Method() {
            this(null, null);
        }

        Method(List<String> countryCodes) {
            this(countryCodes, null);
        }

        /**
         * @param countryCodes  If countryCodes is null we use all countries
         * @param currencyCodes If currencyCodes is  null we create it from the countries
         */
        Method(@Nullable List<String> countryCodes, @Nullable List<String> currencyCodes) {
            countries = countryCodes != null ?
                    CountryRepository.getCountriesFromCodes(countryCodes) :
                    CountryRepository.getCountries();
            countries.sort(Comparator.comparing(Country::getName));

            this.tradeCurrencies = currencyCodes != null ?
                    toTradeCurrencies(currencyCodes) :
                    countries.stream()
                            .map(country -> FiatCurrencyRepository.getCurrencyByCountryCode(country.getCode()))
                            .distinct()
                            .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                            .collect(Collectors.toList());
            this.tradeCurrencies.sort(Comparator.comparing(TradeCurrency::getName));

            this.currencyCodes = currencyCodes != null ?
                    currencyCodes :
                    tradeCurrencies.stream().map(TradeCurrency::getCode).collect(Collectors.toList());
            // sorting this.currencyCodes throws an ExceptionInInitializerError. Not clear why. 
            // But currencyCodes comes from hard coded values, so it is deterministic anyway.
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Class instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public FiatPayment(Method method) {
        super(method);
    }

    public FiatPayment(String paymentMethodName) {
        super(paymentMethodName);
    }

    @Override
    public bisq.account.protobuf.Payment toProto() {
        return getPaymentBuilder().setFiatPayment(bisq.account.protobuf.FiatPayment.newBuilder()).build();
    }

    public static FiatPayment fromProto(bisq.account.protobuf.Payment proto) {
        return FiatPayment.fromName(proto.getPaymentMethodName());
    }

    @Override
    protected FiatPayment.Method getFallbackMethod() {
        return Method.USER_DEFINED;
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return method.getTradeCurrencies();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Payment method specific data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static List<String> getSepaEuroCountries() {
        return List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA");
    }

    private static List<TradeCurrency> toTradeCurrencies(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCode)
                .distinct()
                .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                .collect(Collectors.toList());
    }

    // https://wise.com/help/articles/2571942/what-countriesregions-can-i-send-to
    // https://github.com/bisq-network/proposals/issues/243
    private static List<String> getWiseCountries() {
        List<String> list = new ArrayList<>(List.of("AR", "AU", "BD", "BR", "BG", "CA", "CL", "CN", "CO", "CR", "CZ", "DK", "EG",
                "GE", "GH", "HK", "HU", "IN", "ID", "IL", "JP", "KE", "MY", "MX", "MA", "NP", "NZ", "NO",
                "PK", "PH", "PL", "RO", "SG", "ZA", "KR", "LK", "SE", "CH", "TZ", "TH", "TR", "UG", "UA", "AE",
                "GB", "US", "UY", "VN", "ZM"));
        list.addAll(getSepaEuroCountries());
        return list;
    }

    private static List<String> getWiseCurrencies() {
        return List.of(
                "AED",
                "ARS",
                "AUD",
                "BGN",
                "CAD",
                "CHF",
                "CLP",
                "CZK",
                "DKK",
                "EGP",
                "EUR",
                "GBP",
                "GEL",
                "HKD",
                "HRK",
                "HUF",
                "IDR",
                "ILS",
                "JPY",
                "KES",
                "KRW",
                "MAD",
                "MXN",
                "MYR",
                "NOK",
                "NPR",
                "NZD",
                "PEN",
                "PHP",
                "PKR",
                "PLN",
                "RON",
                "RUB",
                "SEK",
                "SGD",
                "THB",
                "TRY",
                "UGX",
                "VND",
                "XOF",
                "ZAR",
                "ZMW"
        );
    }

    // https://www.revolut.com/help/getting-started/exchanging-currencies/what-fiat-currencies-are-supported-for-holding-and-exchange
    private static List<String> getRevolutCountries() {
        return List.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US");
    }

    private static List<String> getRevolutCurrencies() {
        return List.of(
                "AED",
                "AUD",
                "BGN",
                "CAD",
                "CHF",
                "CZK",
                "DKK",
                "EUR",
                "GBP",
                "HKD",
                "HRK",
                "HUF",
                "ILS",
                "ISK",
                "JPY",
                "MAD",
                "MXN",
                "NOK",
                "NZD",
                "PLN",
                "QAR",
                "RON",
                "RSD",
                "RUB",
                "SAR",
                "SEK",
                "SGD",
                "THB",
                "TRY",
                "USD",
                "ZAR"
        );
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