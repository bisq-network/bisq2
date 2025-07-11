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

import bisq.common.application.ApplicationVersion;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.currency.TradeCurrency;
import bisq.common.data.Pair;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.threading.ExecutorFactory;
import bisq.common.threading.ThreadName;
import bisq.common.timer.Scheduler;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.common.network.TransportType;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.utils.HttpException;
import com.google.gson.Gson;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;


@Slf4j
public class MarketPriceRequestService {
    private static final ExecutorService POOL = ExecutorFactory.newFixedThreadPool("MarketPrice", 3);

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config typesafeConfig) {
            Set<Provider> providers = typesafeConfig.getConfigList("providers").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new Provider(url, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());

            Set<Provider> fallbackProviders = typesafeConfig.getConfigList("fallbackProviders").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new Provider(url, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());

            long interval = typesafeConfig.getLong("interval");
            long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
            return new MarketPriceRequestService.Config(providers, fallbackProviders, interval, timeoutInSeconds);
        }

        private static TransportType getTransportTypeFromUrl(String url) {
            if (url.endsWith(".i2p")) {
                return TransportType.I2P;
            } else if (url.endsWith(".onion")) {
                return TransportType.TOR;
            } else {
                return TransportType.CLEAR;
            }
        }

        private final Set<Provider> providers;
        private final Set<Provider> fallbackProviders;
        private final long interval;
        private final long timeoutInSeconds;

        public Config(Set<Provider> providers, Set<Provider> fallbackProviders, long interval, long timeoutInSeconds) {
            this.providers = providers;
            this.fallbackProviders = fallbackProviders;
            this.interval = interval;
            this.timeoutInSeconds = timeoutInSeconds;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        private final String baseUrl;
        private final String operator;
        private final TransportType transportType;

        public Provider(String baseUrl, String operator, TransportType transportType) {
            this.baseUrl = baseUrl;
            this.operator = operator;
            this.transportType = transportType;
        }
    }

    private final Config conf;
    private final NetworkService networkService;
    @Getter
    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private final String userAgent;
    private Optional<BaseHttpClient> httpClient = Optional.empty();
    @Nullable
    private Scheduler scheduler;
    private long initialDelay = 0;
    @Getter
    private Optional<Provider> mostRecentProvider = Optional.empty();
    private final AtomicReference<Provider> selectedProvider = new AtomicReference<>();
    private final Set<Provider> candidates = new HashSet<>();
    private final Set<Provider> providersFromConfig = new HashSet<>();
    private final Set<Provider> fallbackProviders = new HashSet<>();
    private final Set<Provider> failedProviders = new HashSet<>();
    private final int numTotalCandidates;
    private long timeSinceLastResponse;
    private final boolean noProviderAvailable;
    private volatile boolean shutdownStarted;

    public MarketPriceRequestService(Config conf,
                                     NetworkService networkService) {
        this.conf = conf;
        this.networkService = networkService;
        userAgent = "bisq-v2/" + ApplicationVersion.getVersion().toString();

        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        conf.providers.stream()
                .filter(provider -> supportedTransportTypes.contains(provider.getTransportType()))
                .forEach(providersFromConfig::add);
        conf.getFallbackProviders().stream()
                .filter(provider -> supportedTransportTypes.contains(provider.getTransportType()))
                .forEach(fallbackProviders::add);

        if (providersFromConfig.isEmpty()) {
            candidates.addAll(fallbackProviders);
        } else {
            candidates.addAll(providersFromConfig);
        }
        noProviderAvailable = candidates.isEmpty();
        numTotalCandidates = providersFromConfig.size() + fallbackProviders.size();
        if (noProviderAvailable) {
            log.warn("We do not have any matching provider setup for supportedTransportTypes {}", supportedTransportTypes);
        } else {
            selectedProvider.set(selectNextProvider());
        }
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        startRequesting();
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        if (scheduler != null) {
            scheduler.stop();
        }
        return httpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    private void startRequesting() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        scheduler = Scheduler.run(this::periodicRequest)
                .host(this)
                .runnableName("periodicRequest")
                .periodically(initialDelay, conf.getInterval(), TimeUnit.SECONDS);
    }

    private void periodicRequest() {
        request().whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (scheduler != null) {
                    scheduler.stop();
                    scheduler = null;
                }
                // Increase delay (up to 30 sec.) for retry each time it fails by 5 sec.
                initialDelay += 5;
                initialDelay = Math.min(30, initialDelay);
                startRequesting();
            }
        });
    }

    private CompletableFuture<Void> request() {
        try {
            return request(new AtomicInteger(0));
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    private CompletableFuture<Void> request(AtomicInteger recursionDepth) {
        if (noProviderAvailable) {
            throw new RuntimeException("No market price provider available");
        }
        if (shutdownStarted) {
            throw new RuntimeException("Shutdown has already started");
        }

        return CompletableFuture.runAsync(() -> {
                    ThreadName.set(this, "request");
                    Provider provider = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
                    BaseHttpClient client = networkService.getHttpClient(provider.baseUrl, userAgent, provider.transportType);
                    httpClient = Optional.of(client);
                    if (client.hasPendingRequest()) {
                        selectedProvider.set(selectNextProvider());
                        int numRecursions = recursionDepth.incrementAndGet();
                        if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
                            log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                            request(recursionDepth).join();
                        } else {
                            log.warn("We exhausted all possible providers and give up");
                            throw new RuntimeException("We failed at all possible providers and give up");
                        }
                        return;
                    }

                    long ts = System.currentTimeMillis();
                    String param = "getAllMarketPrices";
                    log.info("Request market price from {}", client.getBaseUrl() + "/" + param);
                    try {
                        String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                        log.info("Received market price from {} after {} ms", client.getBaseUrl() + "/" + param, System.currentTimeMillis() - ts);
                        Map<Market, MarketPrice> map = parseResponse(json);

                        if (map.isEmpty()) {
                            log.warn("Provider {} returned an empty or invalid response, switching provider.", client.getBaseUrl());
                            throw new IllegalStateException("Provider is responsive but not returning any market prices");
                        }

                        long now = System.currentTimeMillis();
                        String sinceLastResponse = timeSinceLastResponse == 0 ? "" : "Time since last response: " + (now - timeSinceLastResponse) / 1000 + " sec";
                        log.info("Market price request from {} resulted in {} items took {} ms. {}",
                                client.getBaseUrl(), map.size(), now - ts, sinceLastResponse);
                        timeSinceLastResponse = now;

                        // We only use those market prices for which we have a market in the repository
                        Map<Market, MarketPrice> filtered = map.entrySet().stream()
                                .filter(e -> e.getValue().isValidDate())
                                .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).isPresent())
                                .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).orElseThrow(),
                                        Map.Entry::getValue));
                        marketPriceByCurrencyMap.clear();
                        marketPriceByCurrencyMap.putAll(filtered);
                        mostRecentProvider = Optional.of(selectedProvider.get());
                        selectedProvider.set(selectNextProvider());
                        shutdownHttpClient(client);
                    } catch (Exception e) {
                        shutdownHttpClient(client);
                        if (shutdownStarted) {
                            throw new RuntimeException("Shutdown has already started");
                        }

                        Throwable rootCause = ExceptionUtil.getRootCause(e);
                        log.warn("{} at request: {}", rootCause.getClass().getSimpleName(), ExceptionUtil.getRootCauseMessage(e));
                        failedProviders.add(provider);
                        selectedProvider.set(selectNextProvider());

                        if (rootCause instanceof HttpException httpException) {
                            int responseCode = httpException.getResponseCode();
                            // If not server error we pass the error to the client
                            if (responseCode < 500) {
                                throw new RuntimeException(e);
                            }
                        }
                        int numRecursions = recursionDepth.incrementAndGet();
                        if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
                            log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                            request(recursionDepth).join();
                        } else {
                            log.warn("We exhausted all possible providers and give up");
                            throw new RuntimeException("We failed at all possible providers and give up");
                        }
                    }
                }, POOL)
                .orTimeout(conf.getTimeoutInSeconds(), SECONDS);
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
                    MarketPriceProvider marketPriceProvider = MarketPriceProvider.fromName(provider);
                    MarketPriceProviderInfo marketPriceProviderInfo = new MarketPriceProviderInfo(marketPriceProvider, marketPriceProvider.getDisplayName().orElse(provider));
                    MarketPrice marketPrice = new MarketPrice(priceQuote,
                            timestamp,
                            marketPriceProviderInfo);
                    if (marketPrice.isValidDate()) {
                        marketPrice.setSource(MarketPrice.Source.REQUESTED_FROM_PRICE_NODE);
                        map.put(priceQuote.getMarket(), marketPrice);
                    } else if (!marketPrice.getMarket().getBaseCurrencyCode().equals("DCR")) {
                        // We get an old DCR price from the price servers. Need to be fixed in price server
                        log.warn("We got an outdated market price. {}", marketPrice);
                    }
                }
            } catch (Exception e) {
                // We do not fail the whole request if one entry would be invalid
                log.warn("Market price conversion failed: {} ", obj, e);
            }
        });
        return map;
    }

    private Provider selectNextProvider() {
        if (candidates.isEmpty()) {
            fillCandidates(0);
        }
        Provider selected = CollectionUtil.getRandomElement(candidates);
        candidates.remove(selected);
        return selected;
    }

    private void fillCandidates(int recursionDepth) {
        providersFromConfig.stream()
                .filter(provider -> !failedProviders.contains(provider))
                .forEach(candidates::add);
        if (candidates.isEmpty()) {
            log.info("We do not have any provider which has not already failed. We add the fall back providers to our candidates list.");
            fallbackProviders.stream()
                    .filter(provider -> !failedProviders.contains(provider))
                    .forEach(candidates::add);
        }
        if (candidates.isEmpty()) {
            log.info("All our providers from config and fallback have failed. We reset the failedProviders and fill from scratch.");
            failedProviders.clear();
            if (recursionDepth == 0) {
                fillCandidates(1);
            } else {
                log.error("recursion at fillCandidates");
            }
        }
    }

    private void shutdownHttpClient(BaseHttpClient client) {
        try {
            client.shutdown();
        } catch (Exception ignore) {
        }
    }
}
