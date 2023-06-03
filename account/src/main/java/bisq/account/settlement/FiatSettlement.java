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

package bisq.account.settlement;

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true)
public class FiatSettlement extends Settlement<FiatSettlement.Method> {
    public static List<Method> getSettlementMethods() {
        return List.of(FiatSettlement.Method.values());
    }

    public static FiatSettlement fromName(String settlementMethodName) {
        try {
            return new FiatSettlement(FiatSettlement.Method.valueOf(settlementMethodName));
        } catch (IllegalArgumentException e) {
            return new FiatSettlement(settlementMethodName);
        }
    }


    public static List<FiatSettlement.Method> getSettlementMethodsForProtocolType(ProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
            case BISQ_MULTISIG:
                return FiatSettlement.getSettlementMethods();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
            case LIGHTNING_X:
                throw new IllegalArgumentException("No settlementMethods for that protocolType");
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }

    public static List<String> getPaymentMethodEnumNamesForCode(String currencyCode) {
        return getPaymentMethodsForCode(currencyCode).stream()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public static List<FiatSettlement.Method> getPaymentMethodsForCode(String currencyCode) {
        return FiatSettlement.getSettlementMethods().stream()
                .filter(method -> new HashSet<>(method.getCurrencyCodes()).contains(currencyCode))
                .collect(Collectors.toList());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Settlement.Method {
        USER_DEFINED,
        SEPA(List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA")),
        SEPA_INSTANT(List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA")),
        ZELLE(List.of("US")),
        REVOLUT(List.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US"), getRevolutCurrencies()),
        WISE(new ArrayList<>(), getWiseCurrencies()),
        NATIONAL_BANK(new ArrayList<>(), new ArrayList<>()),
        SWIFT;
        
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
            tradeCurrencies.sort(Comparator.comparing(TradeCurrency::getName));

            this.currencyCodes = currencyCodes != null ?
                    currencyCodes :
                    tradeCurrencies.stream().map(TradeCurrency::getCode).collect(Collectors.toList());
            this.currencyCodes.sort(Comparator.comparing(String::toString));
        }

        public static List<Country> getSepaEuroCountries() {
            List<String> codes = List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                    "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA");
            List<Country> list = CountryRepository.getCountriesFromCodes(codes);
            list.sort(Comparator.comparing(Country::getName));
            return list;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Class instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public FiatSettlement(Method method) {
        super(method);
    }

    public FiatSettlement(String settlementMethodName) {
        super(settlementMethodName);
    }

    @Override
    public bisq.account.protobuf.Settlement toProto() {
        return getSettlementBuilder().setFiatSettlement(bisq.account.protobuf.FiatSettlement.newBuilder()).build();
    }

    public static FiatSettlement fromProto(bisq.account.protobuf.Settlement proto) {
        return FiatSettlement.fromName(proto.getSettlementMethodName());
    }

    @Override
    protected FiatSettlement.Method getFallbackMethod() {
        return Method.USER_DEFINED;
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return method.getTradeCurrencies();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Payment method specific data
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static List<TradeCurrency> toTradeCurrencies(List<String> currencyCodes) {
        return currencyCodes.stream()
                .map(FiatCurrencyRepository::getCurrencyByCode)
                .distinct()
                .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                .collect(Collectors.toList());
    }

    // https://github.com/bisq-network/proposals/issues/243
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
