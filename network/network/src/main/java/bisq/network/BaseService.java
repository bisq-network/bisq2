package bisq.network;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.util.CollectionUtil;
import bisq.network.http.BaseHttpClient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseService implements Service {

    protected final Observable<Provider> selectedProvider = new Observable<>();
    protected final BaseService.Config conf;
    protected final NetworkService networkService;
    protected final String userAgent;
    protected final Set<Provider> candidates = new HashSet<>();
    protected final Set<Provider> providersFromConfig = new HashSet<>();
    protected final Set<Provider> fallbackProviders = new HashSet<>();
    protected final Set<Provider> failedProviders = new HashSet<>();
    protected Optional<BaseHttpClient> httpClient = Optional.empty();
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

    public BaseService(String apiPath, Config conf, NetworkService networkService) {
        conf.apiPath = apiPath;
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

    @Getter
    @ToString
    public static final class Config {
        @Setter
        @Getter
        protected String apiPath;
        public static Config from(com.typesafe.config.Config typesafeConfig) {
            long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
            String apiPath = typesafeConfig.getString("apiPath");
            Set<Provider> providers = typesafeConfig.getConfigList("providers").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new Provider(url, apiPath, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());

            Set<Provider> fallbackProviders = typesafeConfig.getConfigList("fallbackProviders").stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new Provider(url, apiPath, operator, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());
            long interval = typesafeConfig.getLong("interval");
            return new Config(apiPath, timeoutInSeconds, interval, providers, fallbackProviders);
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


        public Config(String apiPath, long timeoutInSeconds, long interval, Set<Provider> providers, Set<Provider> fallbackProviders) {
            this.timeoutInSeconds = timeoutInSeconds;
            this.providers = providers;
            this.fallbackProviders = fallbackProviders;
            this.interval = interval;
            this.apiPath = apiPath;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        protected final String baseUrl;
        protected final String operator;
        protected final String apiPath;
        protected final TransportType transportType;

        public Provider(String baseUrl,
                        String operator,
                        String apiPath,
                        TransportType transportType) {
            this.baseUrl = baseUrl;
            this.operator = operator;
            this.apiPath = apiPath;
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
            log.info("All our providers from config and fallback have failed. We reset the failedProviders and fill from scratch.");
            failedProviders.clear();
            if (recursionDepth == 0) {
                fillCandidates(1);
            } else {
                log.error("recursion at fillCandidates");
            }
        }
    }
}
