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
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.MathUtils;
import bisq.common.util.Version;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.network.http.BaseHttpClient;
import com.google.gson.Gson;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public class MarketPriceRequestService {
    private static final ExecutorService POOL = ExecutorFactory.newFixedThreadPool("MarketPriceService.pool", 3);

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config marketPriceConfig) {
            Set<Provider> marketPriceProviders = marketPriceConfig.getConfigList("providers").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new Provider(url, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());

            long interval = marketPriceConfig.getLong("interval");
            return new MarketPriceRequestService.Config(marketPriceProviders, interval);
        }

        private static TransportType getTransportTypeFromUrl(String url) {
            if (url.endsWith(".i2p/")) {
                return TransportType.I2P;
            } else if (url.endsWith(".onion/")) {
                return TransportType.TOR;
            } else {
                return TransportType.CLEAR;
            }
        }

        private final Set<Provider> providers;
        private final long interval;

        public Config(Set<Provider> providers, long interval) {
            this.providers = providers;
            this.interval = interval;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        private final String url;
        private final String operator;
        private final TransportType transportType;

        public Provider(String url, String operator, TransportType transportType) {
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
    private final long interval;
    private final NetworkService networkService;
    @Getter
    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();

    private final String userAgent;
    private final List<Provider> candidates = new ArrayList<>();
    private Optional<BaseHttpClient> currentHttpClient = Optional.empty();
    private volatile boolean shutdownStarted;
    @Nullable
    private Scheduler scheduler;
    private long initialDelay = 10;

    public MarketPriceRequestService(Config conf,
                                     Version version,
                                     NetworkService networkService) {
        providers = new ArrayList<>(conf.getProviders());
        interval = conf.getInterval();
        checkArgument(!providers.isEmpty(), "providers must not be empty");
        userAgent = "bisq-v2/" + version.toString();
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        startRequesting();

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        shutdownStarted = true;
        if (scheduler != null) {
            scheduler.stop();
        }
        return currentHttpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
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
                                // Increase delay for retry each time it fails by 5 sec.
                                initialDelay += 5;
                                startRequesting();
                            }
                        });
            });
        }).periodically(initialDelay, interval, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> request(BaseHttpClient httpClient) {
        if (httpClient.hasPendingRequest()) {
            return CompletableFuture.failedFuture(new PendingRequestException());
        }

        return CompletableFuture.runAsync(() -> {
            try {
                long ts = System.currentTimeMillis();
                log.info("Request market price from {}", httpClient.getBaseUrl());
                String json = httpClient.get("getAllMarketPrices", Optional.of(new Pair<>("User-Agent", userAgent)));
                Map<Market, MarketPrice> map = parseResponse(json);
                long now = System.currentTimeMillis();
                log.info("Market price request from {} resulted in {} items took {} ms",
                        httpClient.getBaseUrl(), map.size(), now - ts);

                // We only use those market prices for which we have a market in the repository
                Map<Market, MarketPrice> filtered = map.entrySet().stream()
                        .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).isPresent())
                        .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).orElseThrow(),
                                Map.Entry::getValue));
                marketPriceByCurrencyMap.clear();
                marketPriceByCurrencyMap.putAll(filtered);
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
                    String provider = (String) treeMap.get("provider"); // Bisq-Aggregate or name of exchange of price feed
                    // Convert Bisq-Aggregate to BISQAGGREGATE
                    provider = provider.replace("-", "").toUpperCase();

                    double price = (Double) treeMap.get("price");
                    // json uses double for our timestamp long value...
                    // We get milliseconds not seconds
                    long timestamp = MathUtils.doubleToLong((Double) treeMap.get("timestampSec"));
                    // We only get BTC based prices not fiat-fiat or altcoin-altcoin
                    boolean isFiat = TradeCurrency.isFiat(currencyCode);
                    String baseCurrencyCode = isFiat ? "BTC" : currencyCode;
                    String quoteCurrencyCode = isFiat ? currencyCode : "BTC";
                    PriceQuote priceQuote = PriceQuote.fromPrice(price, baseCurrencyCode, quoteCurrencyCode);
                    MarketPrice marketPrice = new MarketPrice(priceQuote,
                            timestamp,
                            MarketPriceProvider.fromName(provider));
                    marketPrice.setSource(MarketPrice.Source.REQUESTED_FROM_PRICE_NODE);
                    map.put(priceQuote.getMarket(), marketPrice);
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
                    .filter(provider -> networkService.getSupportedTransportTypes().contains(TransportType.CLEAR))
                    .filter(provider -> TransportType.CLEAR == provider.transportType)
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
