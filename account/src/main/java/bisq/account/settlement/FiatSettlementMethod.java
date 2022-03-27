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

import bisq.account.protocol.SwapProtocolType;
import bisq.common.currency.FiatCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import com.google.protobuf.ProtocolMessageEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum FiatSettlementMethod implements SettlementMethod {
    SEPA,
    ZELLE,
    REVOLUT,
    BANK,
    OTHER;
    
    public static List<FiatSettlementMethod> getSettlementMethods(SwapProtocolType protocolType) {
        return switch (protocolType) {
            case BTC_XMR_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case LIQUID_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case BSQ_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case LN_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case MULTISIG -> List.of(FiatSettlementMethod.values());
            case BSQ_BOND -> List.of(FiatSettlementMethod.values());
            case REPUTATION -> List.of(FiatSettlementMethod.values());
        };
    }

    public static List<TradeCurrency> getTradeCurrencies(FiatSettlementMethod settlement) {
        return switch (settlement) {
            case SEPA -> getSepaTradeCurrencies();
            case ZELLE -> List.of(FiatCurrencyRepository.getCurrencyByCode("USD"));
            case REVOLUT -> getRevolutCurrencies();
            case BANK -> new ArrayList<>(FiatCurrencyRepository.getAllCurrencies());
            case OTHER -> new ArrayList<>(FiatCurrencyRepository.getAllCurrencies());
        };
    }

    private static List<TradeCurrency> getRevolutCurrencies() {
        return getRevolutCountries().stream()
                .map(country -> FiatCurrencyRepository.getCurrencyByCountryCode(country.code()))
                .collect(Collectors.toList());
    }

    private static List<TradeCurrency> getSepaTradeCurrencies() {
        return getSepaEuroCountries().stream()
                .map(country -> FiatCurrencyRepository.getCurrencyByCountryCode(country.code()))
                .collect(Collectors.toList());
    }


    public static List<Country> getCountries(FiatSettlementMethod settlement) {
        return switch (settlement) {
            case SEPA -> getSepaEuroCountries();
            case ZELLE -> new ArrayList<>();
            case REVOLUT -> getRevolutCountries();
            case BANK -> new ArrayList<>();
            case OTHER -> new ArrayList<>();
        };
    }

    public static List<Country> getSepaEuroCountries() {
        List<String> codes = List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA");
        List<Country> list = CountryRepository.getCountriesFromCodes(codes);
        list.sort(Comparator.comparing(Country::name));
        return list;
    }

    public static List<Country> getRevolutCountries() {
        List<String> codes = List.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US");
        List<Country> list = CountryRepository.getCountriesFromCodes(codes);
        list.sort(Comparator.comparing(Country::name));
        return list;
    }

    @Override
    public ProtocolMessageEnum toProto() {
        return null;
    }
}
