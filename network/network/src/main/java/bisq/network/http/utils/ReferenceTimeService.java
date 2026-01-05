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

package bisq.network.http.utils;

import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.data.Pair;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Derived from ExplorerService
 */
@Slf4j
public class ReferenceTimeService implements Service {
    private static final ExecutorService POOL = ExecutorFactory.newCachedThreadPool("ReferenceTimeService", 1, 4, 60);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static class RetryException extends RuntimeException {
        @Getter
        private final AtomicInteger recursionDepth;

        public RetryException(String message, AtomicInteger recursionDepth) {
            super(message);
            this.recursionDepth = recursionDepth;
        }
    }

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config typesafeConfig) {
            long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
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
            return new Config(timeoutInSeconds, providers, fallbackProviders);
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
        private final long timeoutInSeconds;

        public Config(long timeoutInSeconds, Set<Provider> providers, Set<Provider> fallbackProviders) {
            this.timeoutInSeconds = timeoutInSeconds;
            this.providers = providers;
            this.fallbackProviders = fallbackProviders;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        private final String baseUrl;
        private final String operator;
        private final String apiPath;
        private final TransportType transportType;

        public Provider(String baseUrl, String operator, TransportType transportType) {
            // Returns json with server time and major fiat prices. We are only interested in the server time.
            this(baseUrl, operator, "api/v1/prices", transportType);
        }

        public Provider(String baseUrl,
                        String operator,
                        String apiPath,
                        TransportType transportType) {
            this.baseUrl = baseUrl;
            this.operator = operator;
            this.apiPath = apiPath;
            this.transportType = transportType;
        }
    }

    @Getter
    private final Observable<Provider> selectedProvider = new Observable<>();
    private final ReferenceTimeService.Config conf;
    private final NetworkService networkService;
    private final String userAgent;
    private final Set<Provider> candidates = new HashSet<>();
    private final Set<Provider> providersFromConfig = new HashSet<>();
    private final Set<Provider> fallbackProviders = new HashSet<>();
    private final Set<Provider> failedProviders = new HashSet<>();
    private Optional<BaseHttpClient> httpClient = Optional.empty();
    private final int numTotalCandidates;
    private final boolean noProviderAvailable;
    private volatile boolean shutdownStarted;

    public ReferenceTimeService(Config conf, NetworkService networkService) {
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

    @Override
    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        return httpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    public CompletableFuture<Long> request() {
        try {
            return request(new AtomicInteger(0))
                    .exceptionallyCompose(throwable -> {
                        if (throwable instanceof RetryException retryException) {
                            return request(retryException.getRecursionDepth());
                        } else if (ExceptionUtil.getRootCause(throwable) instanceof RetryException retryException) {
                            return request(retryException.getRecursionDepth());
                        } else {
                            return CompletableFuture.failedFuture(throwable);
                        }
                    });
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    public String getSelectedProviderBaseUrl() {
        return Optional.ofNullable(selectedProvider.get()).map(ReferenceTimeService.Provider::getBaseUrl).orElse(Res.get("data.na"));
    }

    private CompletableFuture<Long> request(AtomicInteger recursionDepth) {
        if (noProviderAvailable) {
            return CompletableFuture.failedFuture(new RuntimeException("No provider available"));
        }
        if (shutdownStarted) {
            return CompletableFuture.failedFuture(new RuntimeException("Shutdown has already started"));
        }
        try {
            return CompletableFuture.supplyAsync(() -> {
                        Provider provider = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
                        BaseHttpClient client = networkService.getHttpClient(provider.baseUrl, userAgent, provider.transportType);
                        httpClient = Optional.of(client);
                        long ts = System.currentTimeMillis();
                        String param = provider.getApiPath();
                        try {
                            log.info("Request reference time from {}", client.getBaseUrl() + "/" + param);
                            String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                            JsonNode timeNode = MAPPER.readTree(json).get("time");
                            if (timeNode == null || timeNode.isNull()) {
                                throw new RuntimeException("Response JSON missing 'time' field");
                            }
                            long time = timeNode.asLong() * 1000;
                            log.info("Received reference time {} from {}/{} after {} ms", new Date(time), client.getBaseUrl(), param, System.currentTimeMillis() - ts);
                            selectedProvider.set(selectNextProvider());
                            shutdownHttpClient(client);
                            return time;
                        } catch (Exception e) {
                            shutdownHttpClient(client);
                            if (shutdownStarted) {
                                throw new RuntimeException("Shutdown has already started");
                            }

                            Throwable rootCause = ExceptionUtil.getRootCause(e);
                            log.warn("Encountered exception requesting reference time from provider {}", provider.getBaseUrl(), rootCause);

                            if (rootCause instanceof HttpException httpException) {
                                int responseCode = httpException.getResponseCode();
                                // If not server error we pass the error to the client
                                // 408 (Request Timeout) and 429 (Too Many Requests) are usually transient
                                // and should rotate to another provider.
                                if (responseCode < 500 && responseCode != 408 && responseCode != 429) {
                                    throw new CompletionException(e);
                                }
                            }

                            int numRecursions = recursionDepth.incrementAndGet();
                            if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
                                failedProviders.add(provider);
                                selectedProvider.set(selectNextProvider());
                                log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                                throw new RetryException("Retrying with next provider", recursionDepth);
                            } else {
                                log.warn("We exhausted all possible providers and give up");
                                throw new RuntimeException("We failed at all possible providers and give up");
                            }
                        }
                    }, POOL)
                    .completeOnTimeout(null, conf.getTimeoutInSeconds(), SECONDS)
                    .thenCompose(time -> {
                        if (time == null) {
                            return CompletableFuture.failedFuture(new RetryException("Timeout", recursionDepth));
                        }
                        return CompletableFuture.completedFuture(time);
                    });
        } catch (RejectedExecutionException e) {
            log.error("Executor rejected requesting reference time task.", e);
            return CompletableFuture.failedFuture(e);
        }
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
