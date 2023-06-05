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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true)
public class FiatSettlement extends Settlement<FiatSettlement.Method> {
    public static List<Method> getSettlementMethods() {
        return List.of(FiatSettlement.Method.values());
    }

    public static FiatSettlement from(String settlementMethodName) {
        try {
            return new FiatSettlement(FiatSettlement.Method.valueOf(settlementMethodName));
        } catch (IllegalArgumentException e) {
            return new FiatSettlement(settlementMethodName);
        }
    }


    public static List<FiatSettlement.Method> getSettlementMethods(ProtocolType protocolType) {
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Settlement.Method {
        USER_DEFINED,
        SEPA(List.of("AT", "BE", "CY", "DE", "EE", "FI", "FR", "GR", "IE",
                "IT", "LV", "LT", "LU", "MC", "MT", "NL", "PT", "SK", "SI", "ES", "AD", "SM", "VA")),
        ZELLE(List.of("US")),
        REVOLUT(List.of("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                "DE", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL",
                "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB",
                "AU", "CA", "SG", "CH", "US")),
        NATIONAL_BANK_TRANSFER;

        @Getter
        private final List<Country> countries;

        Method() {
            countries = new ArrayList<>();
        }

        Method(List<String> countryCodes) {
            countries = CountryRepository.getCountriesFromCodes(countryCodes);
            countries.sort(Comparator.comparing(Country::getName));
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
    protected FiatSettlement.Method getFallbackMethod() {
        return Method.USER_DEFINED;
    }

    @Override
    public bisq.account.protobuf.Settlement toProto() {
        return getSettlementBuilder().setFiatSettlement(bisq.account.protobuf.FiatSettlement.newBuilder()).build();
    }

    public static FiatSettlement fromProto(bisq.account.protobuf.Settlement proto) {
        return FiatSettlement.from(proto.getSettlementMethodName());
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return method.getCountries().stream()
                .map(country -> FiatCurrencyRepository.getCurrencyByCountryCode(country.getCode()))
                .sorted(Comparator.comparingInt(TradeCurrency::hashCode))
                .collect(Collectors.toList());
    }
}
