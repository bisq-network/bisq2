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
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Derived from ExplorerService
 */
@Slf4j
public abstract class HttpRequestService<T, R> implements Service {
    private static class RetryException extends RuntimeException {
        @Getter
        private final AtomicInteger recursionDepth;

        public RetryException(String message, AtomicInteger recursionDepth) {
            super(message);
            this.recursionDepth = recursionDepth;
        }
    }

    private final ExecutorService executorService;
    @Getter
    private final Observable<HttpRequestUrlProvider> selectedProvider = new Observable<>();
    private final HttpRequestServiceConfig conf;
    private final NetworkService networkService;
    private final String userAgent;
    private final Set<HttpRequestUrlProvider> candidates = new HashSet<>();
    private final Set<HttpRequestUrlProvider> providersFromConfig = new HashSet<>();
    private final Set<HttpRequestUrlProvider> fallbackProviders = new HashSet<>();
    private final Set<HttpRequestUrlProvider> failedProviders = new HashSet<>();
    private Optional<BaseHttpClient> httpClient = Optional.empty();
    private final int numTotalCandidates;
    private final boolean noProviderAvailable;
    private volatile boolean shutdownStarted;

    public HttpRequestService(HttpRequestServiceConfig conf, NetworkService networkService,
                              ExecutorService executorService) {
        this.conf = conf;
        this.networkService = networkService;
        this.executorService = executorService;

        userAgent = "bisq-v2/" + ApplicationVersion.getVersion().toString();

        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        conf.getProviders().stream()
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

    public CompletableFuture<R> request(T requestData) {
        try {
            return request(requestData, new AtomicInteger(0))
                    .exceptionallyCompose(throwable -> {
                        if (throwable instanceof RetryException retryException) {
                            return request(requestData, retryException.getRecursionDepth());
                        } else if (ExceptionUtil.getRootCause(throwable) instanceof RetryException retryException) {
                            return request(requestData, retryException.getRecursionDepth());
                        } else {
                            return CompletableFuture.failedFuture(throwable);
                        }
                    });
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    public String getSelectedProviderBaseUrl() {
        return Optional.ofNullable(selectedProvider.get()).map(HttpRequestUrlProvider::getBaseUrl).orElse(Res.get("data.na"));
    }

    protected abstract R parseResult(String json) throws JsonProcessingException;

    protected abstract String getParam(HttpRequestUrlProvider provider, T requestData);


    private CompletableFuture<R> request(T request, AtomicInteger recursionDepth) {
        if (noProviderAvailable) {
            return CompletableFuture.failedFuture(new RuntimeException("No provider available"));
        }
        if (shutdownStarted) {
            return CompletableFuture.failedFuture(new RuntimeException("Shutdown has already started"));
        }
        try {
            return CompletableFuture.supplyAsync(() -> {
                        HttpRequestUrlProvider provider = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
                        BaseHttpClient client = networkService.getHttpClient(provider.getBaseUrl(), userAgent, provider.getTransportType());
                        httpClient = Optional.of(client);
                        long ts = System.currentTimeMillis();
                        String param = getParam(provider, request);
                        try {
                            log.info("Request reference time from {}", client.getBaseUrl() + "/" + param);

                            String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));

                            R result = parseResult(json);

                            log.info("Received result {} from {}/{} after {} ms", result, client.getBaseUrl(), param, System.currentTimeMillis() - ts);
                            selectedProvider.set(selectNextProvider());
                            shutdownHttpClient(client);
                            return result;
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
                    }, executorService)
                    .completeOnTimeout(null, conf.getTimeoutInSeconds(), SECONDS)
                    .thenCompose(result -> {
                        if (result == null) {
                            return CompletableFuture.failedFuture(new RetryException("Timeout", recursionDepth));
                        }
                        return CompletableFuture.completedFuture(result);
                    });
        } catch (RejectedExecutionException e) {
            log.error("Executor rejected requesting reference time task.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private HttpRequestUrlProvider selectNextProvider() {
        if (candidates.isEmpty()) {
            fillCandidates(0);
        }
        HttpRequestUrlProvider selected = CollectionUtil.getRandomElement(candidates);
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
