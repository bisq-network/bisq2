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

import bisq.common.asset.Asset;
import bisq.common.data.Pair;
import bisq.common.file.FileReaderUtils;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.network.HttpRequestBaseService;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;


@Slf4j
public class MarketPriceRequestService extends HttpRequestBaseService {
    private static final ExecutorService EXECUTOR = ExecutorFactory.newSingleThreadExecutor("MarketPriceRequestService");

    @Getter
    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    @Nullable
    private Scheduler scheduler;
    private long initialDelay = 0;
    @Getter
    private Optional<Provider> mostRecentProvider = Optional.empty();

    public MarketPriceRequestService(Config conf, NetworkService networkService) {
        super("getAllMarketPrices", conf, networkService, EXECUTOR);
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

        ExecutorFactory.shutdownAndAwaitTermination(EXECUTOR, 100);

        return httpClient.get().map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    Map<Market, MarketPrice> loadStaticDevMarketPrice() {
        String resourceName = "dev_market_price.json";
        try {
            String json = FileReaderUtils.readStringFromResource(resourceName);
            Map<Market, MarketPrice> map = parseResponse(json);
            log.warn("We applied developer market price data from resources. " +
                    "This data is outdated and serves only for the case that the clearnet provider is offline.");
            return map.entrySet().stream()
                    .filter(e -> e.getValue().isValidDate())
                    .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).isPresent())
                    .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).orElseThrow(),
                            Map.Entry::getValue));
        } catch (IOException e) {
            log.error("Could not read string from resources: {}", resourceName, e);
            return new HashMap<>();
        }
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
        requestMarketPrice().whenComplete((result, throwable) -> {
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

    private CompletableFuture<Void> requestMarketPrice() {
        return executeWithRetry(() -> makeRequestWithTimeout(provider -> {
            BaseHttpClient client = networkService.getHttpClient(provider.getBaseUrl(), userAgent, provider.getTransportType());
            if (client.hasPendingRequest()) {
                selectedProvider.set(selectNextProvider());
                throw new RetryException("Retrying with next provider due to pending request", new AtomicInteger(1));
            }

            long ts = System.currentTimeMillis();
            String param = provider.getApiPath();
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
            String sinceLastResponse = conf.getTimeSinceLastResponse() == 0 ? "" : "Time since last response: " + (now - conf.getTimeSinceLastResponse()) / 1000 + " sec";
            log.info("Market price request from {} resulted in {} items took {} ms. {}",
                    client.getBaseUrl(), map.size(), now - ts, sinceLastResponse);
            conf.setTimeSinceLastResponse(now);

            // We only use those market prices for which we have a market in the repository
            Map<Market, MarketPrice> filtered = map.entrySet().stream()
                    .filter(e -> e.getValue().isValidDate())
                    .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).isPresent())
                    .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).orElseThrow(),
                            Map.Entry::getValue));
            marketPriceByCurrencyMap.clear();
            marketPriceByCurrencyMap.putAll(filtered);
            mostRecentProvider = Optional.of(selectedProvider.get());
            
            return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private <T> CompletableFuture<T> makeRequestWithTimeout(Function<Provider, CompletableFuture<T>> requestFunction) {
        if (noProviderAvailable) {
            return CompletableFuture.failedFuture(new RuntimeException("No provider available"));
        }
        if (shutdownStarted) {
            return CompletableFuture.failedFuture(new RuntimeException("Shutdown has already started"));
        }

        return CompletableFuture.supplyAsync(() -> {
                    Provider provider = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
                    BaseHttpClient client = networkService.getHttpClient(provider.getBaseUrl(), userAgent, provider.getTransportType());
                    httpClient.set(Optional.of(client));

                    try {
                        T result = requestFunction.apply(provider).join();
                        selectedProvider.set(selectNextProvider());
                        shutdownHttpClient(client);
                        return result;
                    } catch (Exception e) {
                        handleRequestException(e, provider, client);
                        // This line should never be reached since handleRequestException throws
                        throw new RuntimeException("Unexpected error", e);
                    }
                }, executor)
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
                    provider = provider.replace("-", "").toUpperCase(Locale.ROOT);

                    double price = (Double) treeMap.get("price");
                    // json uses double for our timestamp long value...
                    // We get milliseconds not seconds
                    long timestamp = MathUtils.doubleToLong((Double) treeMap.get("timestampSec"));
                    // We only get BTC based prices not fiat-fiat or altcoin-altcoin
                    boolean isFiat = Asset.isFiat(currencyCode);
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
                    } else {
                        if (marketPrice.getMarket().getBaseCurrencyCode().equals("DCR")) {
                            log.warn("We got an outdated market price for DCR and ignore that");
                            // We get an old DCR price from the price servers. Need to be fixed in price server
                        } else {
                            log.warn("We got an outdated market price. timestamp={}\n{}", new Date(marketPrice.getTimestamp()), marketPrice);
                        }
                    }
                }
            } catch (Exception e) {
                // We do not fail the whole request if one entry would be invalid
                log.warn("Market price conversion failed: {} ", obj, e);
            }
        });
        return map;
    }

}
