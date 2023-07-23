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

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.data.Pair;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.MathUtils;
import bisq.common.util.Version;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import com.google.gson.Gson;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class MarketPriceService {
    public static final ExecutorService POOL = ExecutorFactory.newFixedThreadPool("MarketPriceService.pool", 3);
    private static final long INTERVAL = 180;

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config config) {
            //todo move to conf
            return new MarketPriceService.Config(Set.of(
                    new MarketPriceService.Provider("https://price.bisq.wiz.biz/", "wiz", Transport.Type.CLEAR),

                    // Sample I2P tunnel, see docs folder for instructions how to create one
                    // new MarketPriceService.Provider("http://txvrt45gy3d73vubwddvyqvv5ke5b6ycmcohlcypbmpmsjyg4ofa.b32.i2p/", "tunnel-to-wiz", Transport.Type.I2P),

                    new MarketPriceService.Provider("http://wizpriceje6q5tdrxkyiazsgu7irquiqjy2dptezqhrtu7l2qelqktid.onion/", "wiz", Transport.Type.TOR),
                    new MarketPriceService.Provider("http://emzypricpidesmyqg2hc6dkwitqzaxrqnpkdg3ae2wef5znncu2ambqd.onion/", "emzy", Transport.Type.TOR),
                    new MarketPriceService.Provider("http://aprcndeiwdrkbf4fq7iozxbd27dl72oeo76n7zmjwdi4z34agdrnheyd.onion/", "mrosseel", Transport.Type.TOR),
                    new MarketPriceService.Provider("http://devinpndvdwll4wiqcyq5e7itezmarg7rzicrvf6brzkwxdm374kmmyd.onion/", "devinbileck", Transport.Type.TOR),
                    new MarketPriceService.Provider("http://ro7nv73awqs3ga2qtqeqawrjpbxwarsazznszvr6whv7tes5ehffopid.onion/", "alexej996", Transport.Type.TOR)));
        }

        private final Set<Provider> providers;

        public Config(Set<Provider> providers) {
            this.providers = providers;
        }

    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        private final String url;
        private final String operator;
        private final Transport.Type transportType;

        public Provider(String url, String operator, Transport.Type transportType) {
            this.url = url;
            this.operator = operator;
            this.transportType = transportType;
        }
    }

    private static class PendingRequestException extends Exception {
        public PendingRequestException() {
            super("We have a pending request");
        }
    }

    private final List<Provider> providers;
    private final NetworkService networkService;
    private final String userAgent;
    private final List<Provider> candidates = new ArrayList<>();
    private Optional<BaseHttpClient> currentHttpClient = Optional.empty();
    @Getter
    private final Map<Market, MarketPrice> marketPriceByCurrencyMap = new HashMap<>();
    @Getter
    private final Observable<Market> selectedMarket = new Observable<>();
    @Getter
    private final Observable<Long> marketPriceUpdateTimestamp = new Observable<>(-1L);
    private volatile boolean shutdownStarted;
    private Scheduler scheduler;

    public MarketPriceService(Config conf, NetworkService networkService, Version version) {
        providers = new ArrayList<>(conf.providers);
        checkArgument(!providers.isEmpty(), "providers must not be empty");
        this.networkService = networkService;
        userAgent = "bisq-v2/" + version.toString();
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        startRequesting();

        return CompletableFuture.completedFuture(true);
    }

    private void startRequesting() {
        scheduler = Scheduler.run(() -> {
            findNextHttpClient().ifPresent(httpClient -> {
                request(httpClient)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                if (scheduler != null) {
                                    scheduler.stop();
                                }
                                startRequesting();
                            }
                        });
            });
        }).periodically(0, INTERVAL, TimeUnit.SECONDS);
    }

    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        scheduler.stop();
        currentHttpClient.ifPresent(BaseHttpClient::shutdown);
        return CompletableFuture.completedFuture(true);
    }

    public void select(Market market) {
        selectedMarket.set(market);
    }

    public Optional<MarketPrice> findMarketPrice(Market market) {
        MarketPrice marketPrice = marketPriceByCurrencyMap.get(market);
        if (marketPrice == null) {
            log.warn("marketPrice for {} not found.\n" +
                    "Available marketPriceByCurrencyMap={}", market, marketPriceByCurrencyMap);
        }
        return Optional.ofNullable(marketPrice);
    }

    public Optional<PriceQuote> findMarketPriceQuote(Market market) {
        return findMarketPrice(market).stream().map(MarketPrice::getPriceQuote).findAny();
    }

    private CompletableFuture<Map<Market, MarketPrice>> request(BaseHttpClient httpClient) {
        if (httpClient.hasPendingRequest()) {
            return CompletableFuture.failedFuture(new PendingRequestException());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                long ts = System.currentTimeMillis();
                log.info("Request market price from {}", httpClient.getBaseUrl());
                String json = httpClient.get("getAllMarketPrices", Optional.of(new Pair<>("User-Agent", userAgent)));
                Map<Market, MarketPrice> map = parseResponse(json);
                long now = System.currentTimeMillis();
                log.info("Market price request from {} resulted in {} items took {} ms",
                        httpClient.getBaseUrl(), map.size(), now - ts);

                marketPriceByCurrencyMap.clear();
                marketPriceByCurrencyMap.putAll(map);
                if (selectedMarket.get() == null) {
                    selectedMarket.set(map.get(MarketRepository.getDefault()).getMarket());
                }
                marketPriceUpdateTimestamp.set(now);
                return marketPriceByCurrencyMap;
            } catch (IOException e) {
                if (!shutdownStarted) {
                    log.warn("Request to market price provider {} failed. Error={}", httpClient.getBaseUrl(), ExceptionUtil.print(e));
                }
                throw new RuntimeException(e);
            }
        }, POOL);
    }

    private Map<Market, MarketPrice> parseResponse(String json) {
        // size of json is about 8kb
        Map<Market, MarketPrice> map = new HashMap<>();
        Map<?, ?> linkedTreeMap = new Gson().fromJson(json, Map.class);
        List<?> list = (ArrayList<?>) linkedTreeMap.get("data");
        list.forEach(obj -> {
            try {
                Map<?, ?> treeMap = (Map<?, ?>) obj;
                String currencyCode = (String) treeMap.get("currencyCode");
                if (!currencyCode.startsWith("NON_EXISTING_SYMBOL")) {
                    String dataProvider = (String) treeMap.get("provider"); // Bisq-Aggregate or name of exchange of price feed
                    double price = (Double) treeMap.get("price");
                    // json uses double for our timestamp long value...
                    long timestampSec = MathUtils.doubleToLong((Double) treeMap.get("timestampSec"));

                    // We only get BTC based prices not fiat-fiat or altcoin-altcoin
                    boolean isFiat = TradeCurrency.isFiat(currencyCode);
                    String baseCurrencyCode = isFiat ? "BTC" : currencyCode;
                    String quoteCurrencyCode = isFiat ? currencyCode : "BTC";
                    PriceQuote priceQuote = PriceQuote.fromPrice(price, baseCurrencyCode, quoteCurrencyCode);
                    map.put(priceQuote.getMarket(), new MarketPrice(priceQuote, currencyCode, timestampSec * 1000, dataProvider));
                }
            } catch (Throwable t) {
                // We do not fail the whole request if one entry would be invalid
                log.warn("Market price conversion failed: {} ", obj);
                t.printStackTrace();
            }
        });
        return map;
    }

    private Optional<BaseHttpClient> findNextHttpClient() {
        return findProvider().map(this::getNewHttpClient);
    }

    private Optional<Provider> findProvider() {
        if (candidates.isEmpty()) {
            // First try to use the clear net candidate if clear net is supported
            candidates.addAll(providers.stream()
                    .filter(provider -> networkService.getSupportedTransportTypes().contains(Transport.Type.CLEAR))
                    .filter(provider -> Transport.Type.CLEAR == provider.transportType)
                    .collect(Collectors.toList()));
            if (candidates.isEmpty()) {
                candidates.addAll(providers.stream()
                        .filter(provider -> networkService.getSupportedTransportTypes().contains(provider.transportType))
                        .collect(Collectors.toList()));
            }
        }
        if (candidates.isEmpty()) {
            log.warn("No provider is available for the supportedTransportTypes. providers={}, supportedTransportTypes={}",
                    providers, networkService.getSupportedTransportTypes());
            return Optional.empty();
        }
        Provider candidate = Objects.requireNonNull(CollectionUtil.getRandomElement(candidates));
        candidates.remove(candidate);
        return Optional.of(candidate);
    }

    private BaseHttpClient getNewHttpClient(Provider provider) {
        currentHttpClient.ifPresent(BaseHttpClient::shutdown);
        BaseHttpClient newClient = networkService.getHttpClient(provider.url, userAgent, provider.transportType);
        currentHttpClient = Optional.of(newClient);
        return newClient;
    }
}
