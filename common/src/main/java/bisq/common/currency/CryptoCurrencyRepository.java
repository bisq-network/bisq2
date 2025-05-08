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

package bisq.common.currency;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CryptoCurrencyRepository {
    public static final CryptoCurrency BITCOIN = new CryptoCurrency("BTC", "Bitcoin");
    public static final CryptoCurrency XMR = new CryptoCurrency("XMR", "Monero");
    public static final CryptoCurrency L_BTC = new CryptoCurrency("L-BTC", "Liquid-Bitcoin");

    @Getter
    private static final Map<String, String> NAME_BY_CODE = new HashMap<>();

    @Getter
    private static final Map<String, CryptoCurrency> MAJOR_CURRENCIES_BY_CODE = Map.of(
            "XMR", XMR,
            "L-BTC", L_BTC,
            "ETH", new CryptoCurrency("ETH", "Ethereum")
    );
    @Getter
    private static final Map<String, CryptoCurrency> MINOR_CURRENCIES_BY_CODE = Map.of(
            "GRIN", new CryptoCurrency("GRIN", "Grin"),
            "ZEC", new CryptoCurrency("ZEC", "Zcash")
    );
    @Getter
    private static final Map<String, CryptoCurrency> ALL_CURRENCIES_BY_CODE = Stream.concat(
                    MAJOR_CURRENCIES_BY_CODE.entrySet().stream(),
                    MINOR_CURRENCIES_BY_CODE.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    @Getter
    private static final List<CryptoCurrency> ALL_CURRENCIES = ALL_CURRENCIES_BY_CODE.values().stream().toList();

    public static Optional<String> findName(String code) {
        return find(code).map(CryptoCurrency::getName);
    }

    public static Optional<CryptoCurrency> find(String code) {
        return Optional.ofNullable(ALL_CURRENCIES_BY_CODE.get(code));
    }

    public static List<CryptoCurrency> getMajorCurrencies() {
        return MAJOR_CURRENCIES_BY_CODE.values().stream().toList();
    }

    public static List<CryptoCurrency> getMinorCurrencies() {
        return MINOR_CURRENCIES_BY_CODE.values().stream().toList();
    }
}
