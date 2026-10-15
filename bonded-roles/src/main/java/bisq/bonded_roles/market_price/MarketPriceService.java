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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.common.application.Service;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.ReadOnlyObservableMap;

import java.util.Optional;

public interface MarketPriceService extends Service {
    void setSelectedMarket(Market market);

    ReadOnlyObservable<Market> getSelectedMarket();

    Optional<MarketPrice> findMarketPrice(Market market);

    Optional<PriceQuote> findMarketPriceQuote(Market market);

    PriceQuote getMarketPriceQuoteOrThrow(Market market);

    ReadOnlyObservableMap<Market, MarketPrice> getMarketPriceByCurrencyMap();

    boolean hasMarketPrice(Market market);

    Optional<MarketPriceRequestService> getMarketPriceRequestService();

    Optional<AuthorizedBondedRole> getMarketPriceProvidingOracle();
}
