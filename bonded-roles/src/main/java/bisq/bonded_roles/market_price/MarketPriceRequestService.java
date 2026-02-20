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
import bisq.common.file.FileReaderUtils;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.TransportType;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.http.HttpRequestService;
import bisq.network.http.HttpRequestServiceConfig;
import bisq.network.http.HttpRequestUrlProvider;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
public class MarketPriceRequestService extends HttpRequestService<Void, Map<Market, MarketPrice>> {
    @Getter
    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private final long interval;
    @Nullable
    private Scheduler scheduler;
    private long initialDelay = 0;
    private long timeSinceLastResponse;

    private static ExecutorService getExecutorService() {
        return ExecutorFactory.newSingleThreadExecutor(MarketPriceRequestService.class.getSimpleName());
    }

    public MarketPriceRequestService(MarketPriceRequestService.Config conf, NetworkService networkService) {
        super(conf,
                networkService,
                getExecutorService());

        interval = conf.getInterval();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        startRequesting();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        disposeScheduler();
        return super.shutdown();
    }

    private void startRequesting() {
        disposeScheduler();
        scheduler = Scheduler.run(this::periodicRequest)
                .host(this)
                .runnableName("periodicRequest")
                .periodically(initialDelay, interval, TimeUnit.SECONDS);
    }

    private void periodicRequest() {
        request(null)
                .whenComplete((map, throwable) -> {
                    if (throwable != null) {
                        // Increase delay (up to 30 sec.) for retry each time it fails by 5 sec.
                        initialDelay += 5;
                        initialDelay = Math.min(30, initialDelay);
                        log.warn("Failed to fetch market prices, retrying in {} seconds", initialDelay);
                        startRequesting();
                    } else {
                        // We only use those market prices for which we have a market in the repository
                        Map<Market, MarketPrice> filtered = map.entrySet().stream()
                                .filter(e -> e.getValue().isValidDate())
                                .filter(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).isPresent())
                                .collect(Collectors.toMap(e -> MarketRepository.findAnyMarketByMarketCodes(e.getKey().getMarketCodes()).orElseThrow(),
                                        Map.Entry::getValue));
                        marketPriceByCurrencyMap.clear();
                        marketPriceByCurrencyMap.putAll(filtered);
                    }
                });
    }

    @Override
    protected Map<Market, MarketPrice> parseResult(String json) {
        Map<Market, MarketPrice> map = parseResponse(json);
        if (map.isEmpty()) {
            log.warn("Provider {} returned an empty or invalid response, switching provider.", getSelectedProviderBaseUrl());
            throw new IllegalStateException("Provider is responsive but not returning any market prices");
        }
        return map;
    }

    @Override
    protected String getParam(HttpRequestUrlProvider provider, Void requestData) {
        return "getAllMarketPrices";
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

    private void disposeScheduler() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
    }

    @Getter
    @ToString
    public static final class Config extends HttpRequestServiceConfig {
        public static Config from(com.typesafe.config.Config typesafeConfig) {
            Set<HttpRequestUrlProvider> providers = typesafeConfig.getConfigList("providers").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new HttpRequestUrlProvider(url, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());

            Set<HttpRequestUrlProvider> fallbackProviders = typesafeConfig.getConfigList("fallbackProviders").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new HttpRequestUrlProvider(url, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());

            long interval = typesafeConfig.getLong("interval");
            long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
            return new Config(providers, fallbackProviders, interval, timeoutInSeconds);
        }

        private final long interval;

        public Config(Set<HttpRequestUrlProvider> providers,
                      Set<HttpRequestUrlProvider> fallbackProviders,
                      long interval,
                      long timeoutInSeconds) {
            super(timeoutInSeconds, providers, fallbackProviders);
            this.interval = interval;
        }
    }
}
