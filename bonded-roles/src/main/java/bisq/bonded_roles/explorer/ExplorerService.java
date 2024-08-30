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

package bisq.bonded_roles.explorer;

import bisq.bonded_roles.explorer.dto.Tx;
import bisq.common.application.ApplicationVersion;
import bisq.common.data.Pair;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.network.http.BaseHttpClient;
import bisq.network.http.utils.HttpException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

// TODO We should support same registration model via oracle node as used with other nodes

@Slf4j
public class ExplorerService {
    public static final ExecutorService POOL = ExecutorFactory.newCachedThreadPool("BlockExplorerService.pool", 2, 6, 60);

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
        private final String txPath;
        private final String addressPath;
        private final TransportType transportType;

        public Provider(String baseUrl, String operator, TransportType transportType) {
            this(baseUrl, operator, "api/", "tx/", "address/", transportType);
        }

        public Provider(String baseUrl, String operator, String apiPath, String txPath, String addressPath, TransportType transportType) {
            this.baseUrl = baseUrl;
            this.operator = operator;
            this.apiPath = apiPath;
            this.txPath = txPath;
            this.addressPath = addressPath;
            this.transportType = transportType;
        }
    }

    @Getter
    private final Observable<Provider> selectedProvider = new Observable<>();
    private final ExplorerService.Config conf;
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

    public ExplorerService(Config conf, NetworkService networkService) {
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
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        return httpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    public CompletableFuture<Tx> requestTx(String txId) {
        try {
            return requestTx(txId, new AtomicInteger(0));
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new RejectedExecutionException("Too many requests. Try again later."));
        }
    }

    private CompletableFuture<Tx> requestTx(String txId, AtomicInteger recursionDepth) {
        if (noProviderAvailable) {
            throw new RuntimeException("No block explorer provider available");
        }
        if (shutdownStarted) {
            throw new RuntimeException("Shutdown has already started");
        }

        return CompletableFuture.supplyAsync(() -> {
            Provider provider = checkNotNull(selectedProvider.get(), "Selected provider must not be null.");
            BaseHttpClient client = networkService.getHttpClient(provider.baseUrl, userAgent, provider.transportType);
            httpClient = Optional.of(client);
            long ts = System.currentTimeMillis();
            String param = provider.getApiPath() + provider.getTxPath() + txId;
            try {
                log.info("Request tx with ID {} from {}", txId, client.getBaseUrl() + "/" + param);
                String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                log.info("Received tx lookup response from {} after {} ms", client.getBaseUrl() + param, System.currentTimeMillis() - ts);
                selectedProvider.set(selectNextProvider());
                shutdownHttpClient(client);
                return new ObjectMapper().readValue(json, Tx.class);
            } catch (Exception e) {
                shutdownHttpClient(client);
                if (shutdownStarted) {
                    throw new RuntimeException("Shutdown has already started");
                }

                Throwable rootCause = ExceptionUtil.getRootCause(e);
                log.warn("{} at requestTx: {}", rootCause.getClass().getSimpleName(), ExceptionUtil.getRootCauseMessage(e));
                failedProviders.add(provider);
                selectedProvider.set(selectNextProvider());

                if (rootCause instanceof HttpException) {
                    HttpException httpException = (HttpException) rootCause;
                    int responseCode = httpException.getResponseCode();
                    // If not server error we pass the error to the client
                    if (responseCode < 500) {
                        throw new RuntimeException(e);
                    }
                }
                int numRecursions = recursionDepth.incrementAndGet();
                if (numRecursions < numTotalCandidates && failedProviders.size() < numTotalCandidates) {
                    log.warn("We retry the request with new provider {}", selectedProvider.get().getBaseUrl());
                    return requestTx(txId, recursionDepth).join();
                } else {
                    log.warn("We exhausted all possible providers and give up");
                    throw new RuntimeException("We failed at all possible providers and give up");
                }
            }
        }, POOL).orTimeout(conf.getTimeoutInSeconds(), SECONDS);
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
