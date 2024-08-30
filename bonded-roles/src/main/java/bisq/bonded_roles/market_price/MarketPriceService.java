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
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.application.Service;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.encoding.Hex;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Getting the market price data from either the marketPriceRequestService or as AuthorizedMarketPriceData
 * from the P2P network. We update the map in the store with the entries which have a more recent timestamp.
 * We persist the price data so after the first start they are available initially.
 * Client code need to ensure that the price data are not too old to be considered reliable.
 */

@Slf4j
public class MarketPriceService implements Service, PersistenceClient<MarketPriceStore>, AuthorizedBondedRolesService.Listener {
    @Getter
    private final MarketPriceStore persistableStore = new MarketPriceStore();
    @Getter
    private final Persistence<MarketPriceStore> persistence;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    private final MarketPriceRequestService marketPriceRequestService;
    private Pin marketPriceByCurrencyMapPin;
    @Getter
    private Optional<AuthorizedBondedRole> marketPriceProvidingOracle = Optional.empty();

    public MarketPriceService(com.typesafe.config.Config marketPrice,
                              PersistenceService persistenceService,
                              NetworkService networkService,
                              AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        marketPriceRequestService = new MarketPriceRequestService(MarketPriceRequestService.Config.from(marketPrice), networkService);
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        authorizedBondedRolesService.addListener(this);

        setSelectedMarket(MarketRepository.getDefault());

        marketPriceByCurrencyMapPin = marketPriceRequestService.getMarketPriceByCurrencyMap().addObserver(() ->
                applyNewMap(marketPriceRequestService.getMarketPriceByCurrencyMap()));

        return marketPriceRequestService.initialize();
    }

    public CompletableFuture<Boolean> shutdown() {
        if (marketPriceByCurrencyMapPin != null) {
            marketPriceByCurrencyMapPin.unbind();
        }
        authorizedBondedRolesService.removeListener(this);
        return marketPriceRequestService.shutdown();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // AuthorizedBondedRolesService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedMarketPriceData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedMarketPriceData authorizedMarketPriceData = (AuthorizedMarketPriceData) authorizedData.getAuthorizedDistributedData();
                String authorizedDataPubKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
                marketPriceProvidingOracle = authorizedBondedRolesService.getBondedRoles().stream()
                        .map(BondedRole::getAuthorizedBondedRole)
                        .filter(authorizedBondedRole -> authorizedBondedRole.getAuthorizedPublicKey().equals(authorizedDataPubKey))
                        .findAny();
                Map<Market, MarketPrice> map = authorizedMarketPriceData.getMarketPriceByCurrencyMap().entrySet().stream()
                        .peek(e -> e.getValue().setSource(MarketPrice.Source.PROPAGATED_IN_NETWORK))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                applyNewMap(map);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isAuthorized(AuthorizedData authorizedData) {
        // The oracle node runs the MarketPricePropagationService. The BondedRoleType.MARKET_PRICE_NODE runs
        // the market price node which is used for requests from user apps or the MarketPricePropagationService.
        // AuthorizedMarketPriceData provide better resilience for market price data, specially at startup in case the
        // request to the market price node fails.
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.ORACLE_NODE);
    }

    private void applyNewMap(Map<Market, MarketPrice> newMap) {
        if (newMap.isEmpty()) {
            return;
        }

        Map<Market, MarketPrice> mapOfNewEntries = getMapOfNewEntries(newMap);
        if (mapOfNewEntries.isEmpty()) {
            return;
        }
        getMarketPriceByCurrencyMap().putAll(mapOfNewEntries);
        persist();
    }

    // Filters the new map for new entries or entries with a newer timestamp as the existing one.
    private Map<Market, MarketPrice> getMapOfNewEntries(Map<Market, MarketPrice> newMap) {
        Map<Market, MarketPrice> marketPriceByCurrencyMap = getMarketPriceByCurrencyMap();
        return newMap.entrySet().stream()
                .filter(e -> {
                    MarketPrice marketPrice = marketPriceByCurrencyMap.get(e.getKey());
                    if (marketPrice == null) {
                        return true;
                    }
                    return e.getValue().getTimestamp() > marketPrice.getTimestamp();
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
