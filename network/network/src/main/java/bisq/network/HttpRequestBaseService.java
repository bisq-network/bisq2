package bisq.network;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.i18n.Res;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.utils.HttpException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public abstract class HttpRequestBaseService implements Service {

    @Getter
    protected final Observable<Provider> selectedProvider = new Observable<>();
    protected final HttpRequestBaseService.Config conf;
    protected final NetworkService networkService;
    protected final ExecutorService executor;
    protected final String userAgent;
    protected final Set<Provider> candidates = ConcurrentHashMap.newKeySet();
    protected final Set<Provider> providersFromConfig = ConcurrentHashMap.newKeySet();
    protected final Set<Provider> fallbackProviders = ConcurrentHashMap.newKeySet();
    protected final Set<Provider> failedProviders = ConcurrentHashMap.newKeySet();
    protected final AtomicReference<Optional<BaseHttpClient>> httpClient = new AtomicReference<>(Optional.empty());
    protected final int numTotalCandidates;
    protected final boolean noProviderAvailable;
    protected volatile boolean shutdownStarted;

    protected static class RetryException extends RuntimeException {
        @Getter
        private final AtomicInteger recursionDepth;

        public RetryException(String message, AtomicInteger recursionDepth) {
            super(message);
            this.recursionDepth = recursionDepth;
        }
    }

    public HttpRequestBaseService(String apiPath, Config conf, NetworkService networkService, ExecutorService executor) {
        this.conf = conf;
        this.executor = executor;

        this.conf.providers.forEach( e -> { e.setApiPath(apiPath);});
        this.conf.fallbackProviders.forEach( e -> { e.setApiPath(apiPath);});

        this.networkService = networkService;
        userAgent = "bisq-v2/" + ApplicationVersion.getVersion().toString();

        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        conf.providers.stream()
                .filter(provider -> supportedTransportTypes.contains(provider.getTransportType()))
                .forEach(providersFromConfig::add);
        conf.fallbackProviders.stream()
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
        return httpClient.get().map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    public String getSelectedProviderBaseUrl() {
        return Optional.ofNullable(selectedProvider.get()).map(HttpRequestBaseService.Provider::getBaseUrl).orElse(Res.get("data.na"));
    }

    protected void shutdownHttpClient(BaseHttpClient client) {
        try {
            client.shutdown();
        } catch (Exception ignore) {
        }
    }

    protected <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> requestSupplier) {
        return executeWithRetry(requestSupplier, new AtomicInteger(0));
    }

    private <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> requestSupplier, AtomicInteger recursionDepth) {
        try {
            return requestSupplier.get()
                    .exceptionallyCompose(throwable -> {
                        if (throwable instanceof RetryException) {
                            int numRecursions = recursionDepth.incrementAndGet();
                            if (numRecursions < numTotalCandidates) {
                                return executeWithRetry(requestSupplier, recursionDepth);
                            } else {
                                return CompletableFuture.failedFuture(new RuntimeException("We failed at all possible providers and give up"));
                            }
                        } else if (ExceptionUtil.getRootCause(throwable) instanceof RetryException) {
                            int numRecursions = recursionDepth.incrementAndGet();
                            if (numRecursions < numTotalCandidates) {
                                return executeWithRetry(requestSupplier, recursionDepth);
                            } else {
                                return CompletableFuture.failedFuture(new RuntimeException("We failed at all possible providers and give up"));
                            }
                        } else {
                            return CompletableFuture.failedFuture(throwable);
                        }
                    });
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    protected <T> CompletableFuture<T> makeRequest(Function<Provider, CompletableFuture<T>> requestFunction) {
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
                        throw new RuntimeException("Unexpected error", e);
                    }
                }, executor)
                .completeOnTimeout(null, conf.getTimeoutInSeconds(), SECONDS)
                .thenCompose(result -> {
                    if (result == null) {
                        Provider currentProvider = selectedProvider.get();
                        if (currentProvider != null) {
                            failedProviders.add(currentProvider);
                            log.warn("Request to provider {} timed out after {} seconds",
                                    currentProvider.getBaseUrl(), conf.getTimeoutInSeconds());
                        }
                        return CompletableFuture.failedFuture(new RetryException("Timeout", new AtomicInteger(1)));
                    }
                    return CompletableFuture.completedFuture(result);
                });
    }

    protected void handleRequestException(Exception e, Provider provider, BaseHttpClient client) throws RuntimeException {
        shutdownHttpClient(client);
        if (shutdownStarted) {
            throw new RuntimeException("Shutdown has already started");
        }

        Throwable rootCause = ExceptionUtil.getRootCause(e);
        log.warn("Encountered exception from provider {}", provider.getBaseUrl(), rootCause);

        if (rootCause instanceof HttpException httpException) {
            int responseCode = httpException.getResponseCode();
            // If not server error we pass the error to the client
            // 408 (Request Timeout) and 429 (Too Many Requests) are usually transient
            // and should rotate to another provider.
            if (responseCode < 500 && responseCode != 408 && responseCode != 429) {
                throw new CompletionException(e);
            }
        }

        failedProviders.add(provider);
        selectedProvider.set(selectNextProvider());
        log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
        throw new RetryException("Retrying with next provider", new AtomicInteger(1));
    }

    @Getter
    @ToString
    public static final class Config {

        private static Set<Provider> parseProviders(List<? extends com.typesafe.config.Config> configList) {
            return configList.stream().map(config -> {
                    String url = config.getString("url");
                    String operator = config.getString("operator");
                    TransportType transportType = getTransportTypeFromUrl(url);
                    return new Provider(url, null, operator, transportType);
                }).collect(Collectors.toSet());
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            Set<Provider> providers = parseProviders(typesafeConfig.getConfigList("providers"));
            Set<Provider> fallbackProviders = parseProviders(typesafeConfig.getConfigList("fallbackProviders"));
            
            long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
            long interval = typesafeConfig.hasPath("interval") ? typesafeConfig.getLong("interval") : 0;
            return new Config(timeoutInSeconds, interval, providers, fallbackProviders);
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
        private final long interval;
        @Setter
        private long timeSinceLastResponse;


        public Config(long timeoutInSeconds, long interval, Set<Provider> providers, Set<Provider> fallbackProviders) {
            this.timeoutInSeconds = timeoutInSeconds;
            this.providers = providers;
            this.fallbackProviders = fallbackProviders;
            this.interval = interval;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode(exclude = "apiPath")
    public static final class Provider {
        protected final String baseUrl;
        protected final String operator;
        @Setter
        protected String apiPath;
        protected final TransportType transportType;

        public Provider(String baseUrl,
                        @Nullable String apiPath,
                        String operator,
                        TransportType transportType) {
            this.baseUrl = baseUrl;
            this.operator = operator;
            this.apiPath = apiPath != null ? apiPath : "";
            this.transportType = transportType;
        }

        public boolean isClearnetProvider() {
            String fullAddress = baseUrl.replaceFirst("^https?://", "");
            return Address.fromFullAddress(fullAddress).isClearNetAddress();
        }
    }

    protected Provider selectNextProvider() {
        if (candidates.isEmpty()) {
            fillCandidates(0);
        }
        Provider selected = CollectionUtil.getRandomElement(candidates);
        candidates.remove(selected);
        return selected;
    }

    protected void fillCandidates(int recursionDepth) {
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
            log.warn("All our providers from config and fallback have failed. We reset the failedProviders and fill from scratch.");
            failedProviders.clear();
            if (recursionDepth == 0) {
                fillCandidates(1);
            } else {
                log.error("recursion at fillCandidates");
            }
        }
    }
}
