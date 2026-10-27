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

package bisq.common.market;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A market trades one currency against another, so its base and its quote currency differ.
 */
class MarketRepositoryDistinctCurrenciesTest {
    @Test
    void noMarketHasTheSameBaseAndQuoteCurrency() {
        List<Market> markets = Stream.of(MarketRepository.getAllMarkets(),
                        MarketRepository.getMajorMarkets(),
                        MarketRepository.getMajorCryptoAssetMarkets(),
                        MarketRepository.getAllCryptoAssetMarkets(),
                        MarketRepository.getAllNonXMRCryptoCurrencyMarkets(),
                        MarketRepository.getAllXmrMarkets(),
                        MarketRepository.getAllFiatMarkets())
                .flatMap(List::stream)
                .toList();

        assertThat(markets).isNotEmpty();
        assertThat(markets)
                .filteredOn(market -> market.getBaseCurrencyCode().equals(market.getQuoteCurrencyCode()))
                .isEmpty();
    }

    // The market price services keep only the prices of the markets which this lookup finds
    @Test
    void btcBtcIsNotAKnownMarket() {
        assertThat(MarketRepository.findAnyMarketByMarketCodes("BTC/BTC")).isEmpty();
        assertThat(MarketRepository.findAnyMarketByMarketCodes("XMR/BTC")).isPresent();
    }
}
