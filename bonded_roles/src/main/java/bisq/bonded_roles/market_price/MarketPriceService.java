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

import bisq.common.application.Service;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.Version;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class MarketPriceService implements Service, PersistenceClient<MarketPriceStore> {
    @Getter
    private final MarketPriceStore persistableStore = new MarketPriceStore();
    @Getter
    private final Persistence<MarketPriceStore> persistence;
    private final MarketPriceRequestService marketPriceRequestService;

    public MarketPriceService(List<? extends com.typesafe.config.Config> marketPriceServiceProviders,
                              Version version,
                              PersistenceService persistenceService,
                              NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        marketPriceRequestService = new MarketPriceRequestService(MarketPriceRequestService.Config.from(marketPriceServiceProviders),
                version,
                networkService);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        setSelectedMarket(MarketRepository.getDefault());

        marketPriceRequestService.getMarketPriceByCurrencyMap().addObserver(() -> {
            if (marketPriceRequestService.getMarketPriceByCurrencyMap().isEmpty()) {
                return;
            }
            // We do not clear to leave potentially missing entries available. Client code need to check the timestamp
            // to decide if the market price is still relevant.
            getMarketPriceByCurrencyMap().putAll(marketPriceRequestService.getMarketPriceByCurrencyMap());
            persist();
        });

        return marketPriceRequestService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return marketPriceRequestService.shutdown();
    }

    public void setSelectedMarket(Market market) {
        getSelectedMarket().set(market);
        persist();
    }

    public Optional<MarketPrice> findMarketPrice(Market market) {
        return Optional.ofNullable(getMarketPriceByCurrencyMap().get(market));
    }

    public Optional<PriceQuote> findMarketPriceQuote(Market market) {
        return findMarketPrice(market).stream().map(MarketPrice::getPriceQuote).findAny();
    }

    public ObservableHashMap<Market, MarketPrice> getMarketPriceByCurrencyMap() {
        return persistableStore.getMarketPriceByCurrencyMap();
    }

    public Observable<Market> getSelectedMarket() {
        return persistableStore.getSelectedMarket();
    }
}
