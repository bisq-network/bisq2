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
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.http.utils.HttpException;
import bisq.network.http.utils.HttpMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Base service that issues HTTP requests with provider failover, retry, timeout,
 * and HTTP-client lifecycle management. Subclasses implement {@link #buildRequest}
 * to describe a single call (method, path, optional body, optional header) and
 * {@link #parseResult} to decode the response body.
 *
 * <h2>Request methods</h2>
 * Both {@code GET} and {@code POST} are supported. The active method is encoded
 * in the {@link HttpRequest} descriptor returned by {@link #buildRequest}.
 *
 * <h2>Retry &amp; failover semantics</h2>
 * The pipeline classifies failures into two buckets:
 * <ul>
 *   <li><b>Transport-level</b> — the request was never delivered to the server
 *       (busy client, connection refused, DNS, TLS handshake, etc.). These are
 *       <i>always</i> retried against the next provider, for both GET and POST.
 *       Retrying is safe because the server has no state to duplicate.</li>
 *   <li><b>Server-level</b> — the server received the request and either
 *       responded with 5xx / 408 / 429, or the framework timeout fired after the
 *       request was sent. For these, retry is gated on the request method:
 *       <ul>
 *         <li>{@code GET}: always retried (idempotent by HTTP contract).</li>
 *         <li>{@code POST}: retried only when the descriptor declares
 *             {@code retryOnServerError = true}. The default is {@code false}.
 *             Set it to {@code true} only when the server-side operation is
 *             idempotent — a 5xx response means the side effect <i>may</i>
 *             have been applied, and a blind retry would duplicate it.</li>
 *       </ul></li>
 * </ul>
 *
 * <p>4xx responses (other than 408 and 429) bypass retry entirely and are
 * propagated to the caller as a {@link CompletionException} — these indicate
 * caller error, not transient failure.
 *
 * <h2>Lifecycle</h2>
 * The service owns the underlying {@link BaseHttpClient}: callers do not need
 * to manage {@code shutdown()} per request. {@link #shutdown()} drains the
 * executor and the most recently used client.
 *
 * <h2>Logging</h2>
 * The framework logs the resolved request URL at INFO. It uses the descriptor's
 * {@link HttpRequest#logPath()} (defaults to {@code path}) for the path portion.
 * Subclasses whose paths contain device tokens, session identifiers, or other
 * sensitive values MUST pass a redacted {@code logPath} when constructing the
 * descriptor — the framework will not redact {@code path} on its own.
 */
@Slf4j
public abstract class HttpRequestService<T, R> implements Service {
    private static class ProviderFailoverException extends RuntimeException {
        @Getter
        private final AtomicInteger recursionDepth;

        public ProviderFailoverException(String message, AtomicInteger recursionDepth) {
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
    protected final Set<HttpRequestUrlProvider> candidates = new CopyOnWriteArraySet<>();
    protected final Set<HttpRequestUrlProvider> providersFromConfig = new CopyOnWriteArraySet<>();
    protected final Set<HttpRequestUrlProvider> fallbackProviders = new CopyOnWriteArraySet<>();
    protected final Set<HttpRequestUrlProvider> failedProviders = new CopyOnWriteArraySet<>();
    @Getter
    protected Optional<HttpRequestUrlProvider> mostRecentProvider = Optional.empty();
    protected volatile Optional<BaseHttpClient> httpClient = Optional.empty();
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

    protected abstract R parseResult(String json) throws JsonProcessingException;

    /**
     * Describe the HTTP call for the given provider and request data.
     * See {@link HttpRequest#get(String)} / {@link HttpRequest#post(String, String, bisq.common.data.Pair, boolean)}.
     */
    protected abstract HttpRequest buildRequest(HttpRequestUrlProvider provider, T requestData);

    public String getSelectedProviderBaseUrl() {
        return Optional.ofNullable(selectedProvider.get()).map(HttpRequestUrlProvider::getBaseUrl).orElse(Res.get("data.na"));
    }

    public CompletableFuture<R> request(T requestData) {
        try {
            return requestWithFailover(requestData, new AtomicInteger(0));
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    private CompletableFuture<R> requestWithFailover(T requestData, AtomicInteger recursionDepth) {
        return request(requestData, recursionDepth)
                .exceptionallyCompose(throwable -> {
                    ProviderFailoverException providerFailoverException = null;
                    if (throwable instanceof ProviderFailoverException e1) {
                        providerFailoverException = e1;
                    } else if (ExceptionUtil.getRootCause(throwable) instanceof ProviderFailoverException e2) {
                        providerFailoverException = e2;
                    }
                    if (providerFailoverException != null) {
                        return requestWithFailover(requestData, providerFailoverException.getRecursionDepth());
                    }

                    return CompletableFuture.failedFuture(throwable);
                });
    }

    private CompletableFuture<R> request(T requestData, AtomicInteger recursionDepth) {
        if (noProviderAvailable) {
            return CompletableFuture.failedFuture(new RuntimeException("No provider available"));
        }
        if (shutdownStarted) {
            return CompletableFuture.failedFuture(new RuntimeException("Shutdown has already started"));
        }

        HttpRequestUrlProvider providerForThisRequest = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
        HttpRequest httpRequest = buildRequest(providerForThisRequest, requestData);
        try {
            return CompletableFuture.supplyAsync(() -> {
                        // For POST the path is appended to the http-client baseUrl when the
                        // client is constructed, since BaseHttpClient.post(param, header) treats
                        // 'param' as the body. TODO: refactor BaseHttpClient.post to accept
                        // (path, body, header) so the API is symmetric with get(path, header).
                        String clientBaseUrl = httpRequest.method() == HttpMethod.POST
                                ? joinUrl(providerForThisRequest.getBaseUrl(), httpRequest.path())
                                : providerForThisRequest.getBaseUrl();
                        // Redacted variant for logs/exceptions — keeps device tokens, session
                        // ids etc. out of log files even when the underlying HTTP client logs
                        // its baseUrl on errors. Equals clientBaseUrl when nothing is redacted.
                        String clientLogBaseUrl = httpRequest.method() == HttpMethod.POST
                                ? loggableUrl(providerForThisRequest, httpRequest)
                                : providerForThisRequest.getBaseUrl();
                        BaseHttpClient client = networkService.getHttpClient(
                                clientBaseUrl,
                                clientLogBaseUrl,
                                userAgent,
                                providerForThisRequest.getTransportType());

                        if (client.hasPendingRequest()) {
                            // Transport-level: request was never sent. Always safe to retry.
                            boolean shouldRetry = shouldRetry(recursionDepth, providerForThisRequest, false);
                            if (shouldRetry) {
                                log.warn("Client had a pending request. We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                                throw new ProviderFailoverException("Client had a pending request. Retrying with next provider " + selectedProvider.get().getBaseUrl(), recursionDepth);
                            } else {
                                log.warn("Client had a pending request. We exhausted all possible providers and give up. Provider={}",
                                        providerForThisRequest.getBaseUrl());
                                throw new RuntimeException("Client had a pending request. We failed at all possible providers and give up. Provider=" + providerForThisRequest.getBaseUrl());
                            }
                        }

                        httpClient = Optional.of(client);

                        long requestedAt = System.currentTimeMillis();
                        try {
                            log.info("Start Http {} request to {}",
                                    httpRequest.method(),
                                    loggableUrl(providerForThisRequest, httpRequest));

                            String json = execute(client, httpRequest);

                            long receivedAt = System.currentTimeMillis();
                            String sinceLastResponse = timeSinceLastResponse == 0 ? "" : "Time since last response: " + (receivedAt - timeSinceLastResponse) / 1000 + " sec";
                            log.info("Received response from {} after {} ms. {}",
                                    client.getLogBaseUrl(), receivedAt - requestedAt, sinceLastResponse);
                            timeSinceLastResponse = receivedAt;

                            R result = parseResult(json);

                            mostRecentProvider = Optional.of(providerForThisRequest);

                            selectedProvider.set(selectNextProvider());
                            shutdownHttpClient(client);
                            return result;
                        } catch (Exception e) {
                            shutdownHttpClient(client);
                            if (shutdownStarted) {
                                throw new RuntimeException("Shutdown has already started");
                            }

                            Throwable rootCause = ExceptionUtil.getRootCause(e);
                            log.warn("Encountered exception during HTTP request to provider {}", providerForThisRequest.getBaseUrl(), rootCause);

                            // Distinguish HTTP responses from transport-level failures.
                            // HTTP responses came back from the server (4xx/5xx) — server-level.
                            // Anything else (IOException at connect/DNS/TLS, etc.) means the
                            // request was never delivered — transport-level, always retriable.
                            if (rootCause instanceof HttpException httpException) {
                                int responseCode = httpException.getResponseCode();
                                if (responseCode < 500 && responseCode != 408 && responseCode != 429) {
                                    // 4xx (other than 408 / 429): caller error, never retry.
                                    throw new CompletionException(e);
                                }
                                // 5xx / 408 / 429: retry only if the method allows it.
                                if (!isServerErrorRetryAllowed(httpRequest)) {
                                    throw new CompletionException(e);
                                }
                            }
                            // else: transport-level failure → fall through to retry path.

                            boolean shouldRetry = shouldRetry(recursionDepth, providerForThisRequest, true);
                            if (shouldRetry) {
                                throw new ProviderFailoverException("Retrying with next provider " + selectedProvider.get().getBaseUrl(), recursionDepth);
                            } else {
                                throw new RuntimeException("We failed at all possible providers and give up. Provider=" + providerForThisRequest.getBaseUrl());
                            }
                        }
                    }, executorService)
                    .orTimeout(conf.getTimeoutInSeconds(), SECONDS)
                    .exceptionallyCompose(throwable -> {
                        if (ExceptionUtil.getRootCause(throwable) instanceof TimeoutException) {
                            log.warn("Request to provider {} timed out after {} seconds",
                                    providerForThisRequest.getBaseUrl(), conf.getTimeoutInSeconds());
                            if (!isServerErrorRetryAllowed(httpRequest)) {
                                return CompletableFuture.failedFuture(new RuntimeException(
                                        "Timeout. Non-idempotent POST — not retrying. Provider=" + providerForThisRequest.getBaseUrl(),
                                        throwable));
                            }
                            boolean shouldRetry = shouldRetry(recursionDepth, providerForThisRequest, true);
                            Exception exception = shouldRetry
                                    ? new ProviderFailoverException("Timeout. Retrying with next provider " + selectedProvider.get().getBaseUrl(), recursionDepth)
                                    : new RuntimeException("Timeout. We failed at all possible providers and give up. Provider=" + providerForThisRequest.getBaseUrl());
                            return CompletableFuture.failedFuture(exception);
                        }
                        return CompletableFuture.failedFuture(throwable);
                    });
        } catch (RejectedExecutionException e) {
            log.error("Executor rejected request task.", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String execute(BaseHttpClient client, HttpRequest httpRequest) throws Exception {
        return switch (httpRequest.method()) {
            case GET -> client.get(httpRequest.path(), httpRequest.header());
            case POST -> client.post(
                    httpRequest.body().orElseThrow(() -> new IllegalStateException("POST request must carry a body")),
                    httpRequest.header());
        };
    }

    /**
     * Server-level retry is allowed for GET (idempotent by HTTP contract) or for
     * POST when the descriptor opts in via {@code retryOnServerError = true}.
     */
    static boolean isServerErrorRetryAllowed(HttpRequest httpRequest) {
        return httpRequest.method() == HttpMethod.GET || httpRequest.retryOnServerError();
    }

    /**
     * Joins {@code base} and {@code path} into a single URL with exactly one
     * separating slash, regardless of trailing slash on {@code base} or leading
     * slash on {@code path}.
     */
    static String joinUrl(String base, String path) {
        String trimmedBase = base.replaceAll("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return trimmedBase + normalizedPath;
    }

    /**
     * Build the URL string used for INFO logging. Honours the descriptor's
     * {@link HttpRequest#logPath()} so subclasses can redact sensitive path
     * segments (device tokens, session ids, etc.) before they hit log files.
     */
    static String loggableUrl(HttpRequestUrlProvider provider, HttpRequest httpRequest) {
        return joinUrl(provider.getBaseUrl(), httpRequest.logPath());
    }

    private boolean shouldRetry(AtomicInteger recursionDepth,
                                HttpRequestUrlProvider providerForThisRequest,
                                boolean addToFailed) {
        int numRecursions = recursionDepth.incrementAndGet();
        if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
            if (addToFailed) {
                failedProviders.add(providerForThisRequest);
            }
            HttpRequestUrlProvider nextProvider = selectNextProvider();
            selectedProvider.set(nextProvider);

            log.warn("Provider {} failed or was busy, retrying with {}", providerForThisRequest.getBaseUrl(), nextProvider.getBaseUrl());
            return true;
        } else {
            log.warn("We exhausted all possible providers and give up");
            return false;
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
                    .or(() -> fallbackProviders.stream().findFirst())
                    .orElseThrow(() -> new IllegalStateException("No providers available"));
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
