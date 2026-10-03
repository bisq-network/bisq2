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

package bisq.bonded_roles.market_price;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.PersistenceService;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A node uses only positive market prices. The oracle data in the network is one of the places where prices enter the
 * node. The node skips only an invalid price of the data and keeps the data unchanged, because the signature of the
 * oracle node covers all its entries. See docs/specifications/bonded-roles/market-prices.md.
 */
class DefaultMarketPriceServiceTest {
    private static final Market USD_MARKET = MarketRepository.getUSDBitcoinMarket();

    private AuthorizedBondedRolesService authorizedBondedRolesService;
    private DefaultMarketPriceService service;

    @BeforeAll
    static void setUpResources() {
        // MarketPriceProvider reads a display name from the resources
        Res.setAndApplyLanguageTag("en");
    }

    @BeforeEach
    void setUp() {
        authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        when(authorizedBondedRolesService.getBondedRoles()).thenReturn(new ObservableSet<>());
        // Without price requests, the node gets its prices only from the oracle data
        service = new DefaultMarketPriceService(ConfigFactory.parseMap(Map.of("enabled", false)),
                mock(PersistenceService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class),
                authorizedBondedRolesService);
    }

    @Test
    void nonPositivePricesOfOracleDataAreSkippedAndTheOtherPricesAreApplied() {
        long now = System.currentTimeMillis();
        AuthorizedMarketPriceData data = oracleData(List.of(marketPrice(PriceQuote.fromFiatPrice(-1000, "EUR"), now),
                marketPrice(PriceQuote.fromFiatPrice(0, "CHF"), now),
                marketPrice(PriceQuote.fromFiatPrice(60000, "USD"), now)));

        service.onAuthorizedDataAdded(authorizedByOracle(data));

        assertThat(service.getMarketPriceByCurrencyMap()).containsOnlyKeys(USD_MARKET);
        assertThat(data.getMarketPriceByCurrencyMap()).hasSize(3);
    }

    @Test
    void nonPositivePriceDoesNotReplaceTheLastPositivePriceOfItsMarket() {
        long now = System.currentTimeMillis();
        MarketPrice positivePrice = marketPrice(PriceQuote.fromFiatPrice(50000, "EUR"), now - 1000);
        service.onAuthorizedDataAdded(authorizedByOracle(oracleData(List.of(positivePrice))));

        service.onAuthorizedDataAdded(authorizedByOracle(oracleData(List.of(
                marketPrice(PriceQuote.fromFiatPrice(-1000, "EUR"), now)))));

        assertThat(service.findMarketPrice(positivePrice.getMarket())).contains(positivePrice);
    }

    private AuthorizedData authorizedByOracle(AuthorizedMarketPriceData data) {
        AuthorizedData authorizedData = mock(AuthorizedData.class);
        when(authorizedData.getAuthorizedDistributedData()).thenReturn(data);
        when(authorizedData.getAuthorizedPublicKeyBytes()).thenReturn(new byte[]{1});
        when(authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.ORACLE_NODE)).thenReturn(true);
        return authorizedData;
    }

    private static AuthorizedMarketPriceData oracleData(List<MarketPrice> marketPrices) {
        TreeMap<Market, MarketPrice> marketPriceByMarket = new TreeMap<>();
        marketPrices.forEach(marketPrice -> marketPriceByMarket.put(marketPrice.getMarket(), marketPrice));
        return new AuthorizedMarketPriceData(marketPriceByMarket, true);
    }

    private static MarketPrice marketPrice(PriceQuote priceQuote, long timestamp) {
        return new MarketPrice(priceQuote, timestamp, new MarketPriceProviderInfo(MarketPriceProvider.BISQAGGREGATE));
    }
}
