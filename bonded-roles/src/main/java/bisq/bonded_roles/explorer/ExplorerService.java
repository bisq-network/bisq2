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
import bisq.common.data.Pair;
import bisq.common.observable.Observable;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.Version;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.network.http.BaseHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO We should support same registration model via oracle node as used with other nodes

@Slf4j
public class ExplorerService {
    public static final ExecutorService POOL = ExecutorFactory.newFixedThreadPool("BlockExplorerService.pool", 3);

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config config) {
            return new Config(List.of(
                    new Provider("https://mempool.emzy.de/", TransportType.CLEAR), // Only used for  dev testing, not a bonded role
                    new Provider("http://runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion/", TransportType.TOR) // Production node, bonded role
            ));
        }

        private final List<Provider> providers;

        public Config(List<Provider> providers) {
            this.providers = providers;
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Provider {
        private final String baseUrl;
        private final String apiPath;
        private final String txPath;
        private final String addressPath;
        private final TransportType transportType;

        public Provider(String baseUrl, TransportType transportType) {
            this(baseUrl, "api/", "tx/", "address/", transportType);
        }

        public Provider(String baseUrl, String apiPath, String txPath, String addressPath, TransportType transportType) {
            this.baseUrl = baseUrl;
            this.apiPath = apiPath;
            this.txPath = txPath;
            this.addressPath = addressPath;
            this.transportType = transportType;
        }
    }

    @Getter
    private final Observable<Provider> selectedProvider = new Observable<>();
    private final NetworkService networkService;
    private final String userAgent;
    private final Map<TransportType, List<Provider>> supportedProvidersByTransportType;
    private volatile boolean shutdownStarted;

    public ExplorerService(Config conf, NetworkService networkService, Version version) {
        this.networkService = networkService;
        userAgent = "bisq-v2/" + version.toString();

        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        supportedProvidersByTransportType = new HashMap<>();
        conf.providers.stream()
                .filter(provider -> supportedTransportTypes.contains(provider.getTransportType()))
                .forEach(provider -> {
                    TransportType transportType = provider.getTransportType();
                    supportedProvidersByTransportType.putIfAbsent(transportType, new ArrayList<>());
                    supportedProvidersByTransportType.get(transportType).add(provider);
                });

        if (supportedProvidersByTransportType.isEmpty()) {
            log.warn("No providers set up for supported transport types {}. " +
                            "conf.providers={}; supportedProvidersByTransportType={}",
                    supportedTransportTypes, conf.providers, supportedProvidersByTransportType);
        } else {
            Provider provider = getRandomProvider(supportedProvidersByTransportType);
            selectedProvider.set(provider);
        }
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        shutdownStarted = true;
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Tx> requestTx(String txId) {
        return CompletableFuture.supplyAsync(() -> {
            Provider provider = selectedProvider.get();
            checkNotNull(provider, "No provider was selected");
            BaseHttpClient client = networkService.getHttpClient(provider.baseUrl, userAgent, provider.transportType);
            long ts = System.currentTimeMillis();
            String param = provider.getApiPath() + provider.getTxPath() + txId;
            try {
                String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                log.info("Requesting tx from {} took {} ms", client.getBaseUrl() + param, System.currentTimeMillis() - ts);
                return new ObjectMapper().readValue(json, Tx.class);
            } catch (Exception e) {
                if (!shutdownStarted) {
                    log.warn(ExceptionUtil.getMessageOrToString(e));
                }
                throw new RuntimeException(e);
            } finally {
                // select random provider for next call
                selectedProvider.set(getRandomProvider(supportedProvidersByTransportType));
                client.shutdown();
            }
        }, POOL);
    }

    private static Provider getRandomProvider(Map<TransportType, List<Provider>> supportedProvidersByTransportType) {
        List<Provider> allProviders = supportedProvidersByTransportType.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return CollectionUtil.getRandomElement(allProviders);
    }
}
