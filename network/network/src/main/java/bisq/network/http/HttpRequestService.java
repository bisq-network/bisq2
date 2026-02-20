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

package bisq.network.http;

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
import bisq.network.http.utils.HttpException;
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

    protected final ExecutorService executorService;
    protected final HttpRequestServiceConfig conf;
    protected final NetworkService networkService;
    protected final String userAgent;

    @Getter
    protected final Observable<HttpRequestUrlProvider> selectedProvider = new Observable<>();
    protected final Set<HttpRequestUrlProvider> candidates = new HashSet<>();
    protected final Set<HttpRequestUrlProvider> providersFromConfig = new HashSet<>();
    protected final Set<HttpRequestUrlProvider> fallbackProviders = new HashSet<>();
    protected final Set<HttpRequestUrlProvider> failedProviders = new HashSet<>();
    @Getter
    protected Optional<HttpRequestUrlProvider> mostRecentProvider = Optional.empty();
    protected Optional<BaseHttpClient> httpClient = Optional.empty();
    protected final int numTotalCandidates;
    protected final boolean noProviderAvailable;
    protected volatile boolean shutdownStarted;
    private volatile long timeSinceLastResponse;

    public HttpRequestService(HttpRequestServiceConfig conf,
                              NetworkService networkService,
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
                .orElse(CompletableFuture.completedFuture(true))
                .thenApply(result -> {
                    ExecutorFactory.shutdownAndAwaitTermination(executorService, 100);
                    return result;
                });
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

                        if (client.hasPendingRequest()) {
                            selectedProvider.set(selectNextProvider());
                            int numRecursions = recursionDepth.incrementAndGet();
                            if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
                                log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                                throw new RetryException("Client busy, retrying with next provider", recursionDepth);
                            } else {
                                log.warn("We exhausted all possible providers and give up");
                                throw new RuntimeException("We failed at all possible providers and give up");
                            }
                        }

                        httpClient = Optional.of(client);

                        long requestedAt = System.currentTimeMillis();
                        String param = getParam(provider, request);
                        try {
                            log.info("Start Http request to {}", client.getBaseUrl());

                            String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));

                            long receivedAt = System.currentTimeMillis();
                            String sinceLastResponse = timeSinceLastResponse == 0 ? "" : "Time since last response: " + (receivedAt - timeSinceLastResponse) / 1000 + " sec";
                            log.info("Received response from {} after {} ms. {}",
                                    client.getBaseUrl(), receivedAt - requestedAt, sinceLastResponse);
                            timeSinceLastResponse = receivedAt;

                            R result = parseResult(json);

                            mostRecentProvider = Optional.of(selectedProvider.get());

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
                            // Timeout occurred - add provider to failed list before retrying
                            HttpRequestUrlProvider currentProvider = selectedProvider.get();
                            if (currentProvider != null) {
                                failedProviders.add(currentProvider);
                                log.warn("Request to provider {} timed out after {} seconds",
                                        currentProvider.getBaseUrl(), conf.getTimeoutInSeconds());
                            }
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
        // Guard against null return from getRandomElement (can happen if candidates is empty)
        HttpRequestUrlProvider selected = CollectionUtil.getRandomElement(candidates);
        if (selected == null) {
            log.error("No provider available - candidates list is empty after fillCandidates");
            // Return first available provider from config as fallback
            return providersFromConfig.stream().findFirst()
                    .orElseGet(() -> fallbackProviders.stream().findFirst()
                            .orElse(null));
        }
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
