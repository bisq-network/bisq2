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

import java.util.ArrayList;
import java.util.List;

public class CurrencyRepository {

    private static final List<TradeCurrency> allCurrencies = new ArrayList<>();

    public static List<TradeCurrency> getAllCurrencies() {
        if (allCurrencies.isEmpty()) {
            allCurrencies.addAll(FiatCurrencyRepository.getMajorCurrencies());
            allCurrencies.addAll(CryptoCurrencyRepository.getMajorCurrencies());
            allCurrencies.addAll(FiatCurrencyRepository.getMinorCurrencies());
            allCurrencies.addAll(CryptoCurrencyRepository.getMinorCurrencies());
        }
        return allCurrencies;
    }
}
