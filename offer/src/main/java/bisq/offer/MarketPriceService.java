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

package bisq.offer;

import bisq.common.currency.MisqCurrency;
import bisq.common.data.Pair;
import bisq.common.monetary.Quote;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.common.util.CollectionUtil;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.http.common.BaseHttpClient;
import bisq.network.p2p.node.transport.Transport;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import io.reactivex.subjects.BehaviorSubject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


@Slf4j
public class MarketPriceService {

    private static final long REQUEST_INTERVAL_SEC = 180;
    private Optional<ExecutorService> executor = Optional.empty();
    private Optional<BaseHttpClient> httpClient = Optional.empty();

    public static record Config(Set<Provider> providers) {
    }

    public static record Provider(String url, String operator, Transport.Type transportType) {
    }

    private final List<Provider> providers;
    private final List<Provider> candidates = new ArrayList<>();
    private final NetworkService networkService;
    private final String userAgent;
    private Provider provider;

    @Getter
    private final Map<String, MarketPrice> marketPriceByCurrencyMap = new HashMap<>();
    @Getter
    private final BehaviorSubject<Map<String, MarketPrice>> marketPriceSubject;

    public MarketPriceService(Config conf, NetworkService networkService, String version) {
        providers = new ArrayList<>(conf.providers);
        checkArgument(!providers.isEmpty(), "providers must not be empty");
        this.networkService = networkService;
        userAgent = "bisq/" + version;

        marketPriceSubject = BehaviorSubject.create();
    }

    public CompletableFuture<Boolean> initialize() {
        executor = Optional.of(ExecutorFactory.newSingleThreadExecutor("MarketPriceRequest"));
        selectProvider();
        // We start a request but we do not block until response arrives.
        request();
        Scheduler.run(() -> request()
                        .whenComplete((map, throwable) -> {
                            if (map.isEmpty() || throwable != null) {
                                selectProvider();
                                httpClient.ifPresent(BaseHttpClient::shutdown);
                                try {
                                    httpClient = Optional.of(getHttpClient(provider).get());
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                                request();
                            }
                        }))
                .periodically(REQUEST_INTERVAL_SEC, TimeUnit.SECONDS);
        return CompletableFuture.completedFuture(true);
    }

    public void shutdown() {
        executor.ifPresent(ExecutorFactory::shutdownAndAwaitTermination);
        httpClient.ifPresent(BaseHttpClient::shutdown);
    }

    public Optional<MarketPrice> getMarketPrice(String currencyCode) {
        return Optional.ofNullable(marketPriceByCurrencyMap.get(currencyCode));
    }

    public CompletableFuture<Map<String, MarketPrice>> request() {
        if (executor.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        if (httpClient.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }
        return request(httpClient.get(), executor.get());
    }

    private CompletableFuture<Map<String, MarketPrice>> request(BaseHttpClient httpClient, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                while (httpClient.hasPendingRequest() && !Thread.interrupted()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignore) {
                    }
                }
                long ts = System.currentTimeMillis();
                log.info("Request market price from {}", httpClient.getBaseUrl());
                String json = httpClient.get("getAllMarketPrices", Optional.of(new Pair<>("User-Agent", userAgent)));
                LinkedTreeMap<?, ?> map = new Gson().fromJson(json, LinkedTreeMap.class);
                List<?> list = (ArrayList<?>) map.get("data");
                list.forEach(obj -> {
                    try {
                        LinkedTreeMap<?, ?> treeMap = (LinkedTreeMap<?, ?>) obj;
                        String currencyCode = (String) treeMap.get("currencyCode");
                        String dataProvider = (String) treeMap.get("provider"); // Bisq-Aggregate or name of exchange of price feed
                        double price = (Double) treeMap.get("price");
                        // json uses double for our timestamp long value...
                        long timestampSec = MathUtils.doubleToLong((Double) treeMap.get("timestampSec"));

                        // We only get BTC based prices not fiat-fiat or altcoin-altcoin
                        boolean isFiat = MisqCurrency.isFiat(currencyCode);
                        String baseCurrencyCode = isFiat ? "BTC" : currencyCode;
                        String quoteCurrencyCode = isFiat ? currencyCode : "BTC";
                        Quote quote = Quote.fromPrice(price, baseCurrencyCode, quoteCurrencyCode);
                        marketPriceByCurrencyMap.put(currencyCode,
                                new MarketPrice(quote,
                                        timestampSec * 1000,
                                        dataProvider));
                    } catch (Throwable t) {
                        // We do not fail the whole request if one entry would be invalid
                        log.warn("Market price conversion failed: {} ", obj);
                        t.printStackTrace();
                    }
                });
                log.info("Market price request from {} resulted in {} items took {} ms",
                        httpClient.getBaseUrl(), list.size(), System.currentTimeMillis() - ts);
                marketPriceSubject.onNext(marketPriceByCurrencyMap);
                return marketPriceByCurrencyMap;
            } catch (IOException e) {
                e.printStackTrace();
                return new HashMap<>();
            }
        }, executor);
    }

    private void selectProvider() {
        if (candidates.isEmpty()) {
            // First try to use the clear net candidate if clear net is supported
            candidates.addAll(providers.stream()
                    .filter(provider -> networkService.getSupportedTransportTypes().contains(Transport.Type.CLEAR))
                    .filter(provider -> Transport.Type.CLEAR == provider.transportType)
                    .toList());
            if (candidates.isEmpty()) {
                candidates.addAll(providers.stream()
                        .filter(provider -> networkService.getSupportedTransportTypes().contains(provider.transportType))
                        .toList());
            }
        }
        Provider candidate = CollectionUtil.getRandomElement(candidates);
        checkNotNull(candidate);
        candidates.remove(candidate);
        provider = candidate;
        try {
            httpClient = Optional.of(getHttpClient(provider).get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private CompletableFuture<BaseHttpClient> getHttpClient(Provider provider) {
        return networkService.getHttpClient(provider.url, userAgent, provider.transportType);
    }
}
