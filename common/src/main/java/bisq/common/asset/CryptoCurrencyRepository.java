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

package bisq.common.asset;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CryptoCurrencyRepository {
    public static final CryptoCurrency BITCOIN = new CryptoCurrency("BTC", "Bitcoin");
    public static final CryptoCurrency XMR = new CryptoCurrency("XMR", "Monero");
    @Setter // Maybe we allow user to set their preferred default?
    @Getter
    private static CryptoCurrency defaultCurrency = XMR;

    @Getter
    private static final List<CryptoCurrency> MAJOR_CURRENCIES = List.of(
            XMR,
            new CryptoCurrency("ETH", "Ethereum"),
            new CryptoCurrency("LTC", "Litecoin")
    );
    @Getter
    private static final List<CryptoCurrency> MINOR_CURRENCIES = List.of(
            new CryptoCurrency("GRIN", "Grin"),
            new CryptoCurrency("ZEC", "Zcash")
    );

    @Getter
    private static final Map<String, CryptoCurrency> MAJOR_CURRENCIES_BY_CODE = MAJOR_CURRENCIES.stream()
            .collect(Collectors.toMap(Asset::getCode, e -> e));
    @Getter
    private static final Map<String, CryptoCurrency> MINOR_CURRENCIES_BY_CODE = MINOR_CURRENCIES.stream()
            .collect(Collectors.toMap(Asset::getCode, e -> e));
    @Getter
    private static final Map<String, CryptoCurrency> ALL_CURRENCIES_BY_CODE = Stream.concat(
                    MAJOR_CURRENCIES_BY_CODE.entrySet().stream(),
                    MINOR_CURRENCIES_BY_CODE.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    @Getter
    private static final List<CryptoCurrency> ALL_CURRENCIES = Stream.concat(
                    MAJOR_CURRENCIES.stream(),
                    MINOR_CURRENCIES.stream())
            .toList();

    public static Optional<String> findName(String code) {
        return find(code).map(CryptoCurrency::getName);
    }

    public static Optional<CryptoCurrency> find(String code) {
        return Optional.ofNullable(ALL_CURRENCIES_BY_CODE.get(code));
    }

    public static CryptoCurrency findOrCreateCustom(String code) {
        return find(code).orElse(new CryptoCurrency(code));
    }

    public static List<CryptoCurrency> getMajorCurrencies() {
        return MAJOR_CURRENCIES;
    }

    public static List<CryptoCurrency> getMinorCurrencies() {
        return MINOR_CURRENCIES;
    }

    public static List<CryptoCurrency> getAllCurrencies() {
        return ALL_CURRENCIES;
    }
}
