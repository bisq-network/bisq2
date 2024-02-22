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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;

// TODO We should support same registration model via oracle node as used with other nodes

@Slf4j
public class ExplorerService {
    public static final ExecutorService POOL = ExecutorFactory.newFixedThreadPool("BlockExplorerService.pool", 3);

    private volatile boolean shutdownStarted;

    @Getter
    @ToString
    public static final class Config {
        public static Config from(com.typesafe.config.Config config) {
            return new Config(List.of(
                    new Provider("https://mempool.emzy.de/", TransportType.CLEAR), // Only used for  dev testing, not bonded role
                    new Provider("http://runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion/", TransportType.TOR) // Production node, bonded role
            ));
        }

        private final List<Provider> providers;

        public Config(List<Provider> providers) {
            this.providers = providers;
        }
    }

    private static class PendingRequestException extends Exception {
        public PendingRequestException() {
            super("We have a pending request");
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


    private final ArrayList<Provider> providers;
    @Getter
    private final Observable<Provider> selectedProvider = new Observable<>();
    private Optional<BaseHttpClient> httpClient = Optional.empty();
    private final NetworkService networkService;
    private final String userAgent;


    public ExplorerService(Config conf, NetworkService networkService, Version version) {
        providers = new ArrayList<>(conf.providers);
        checkArgument(providers.size() > 0);
        selectedProvider.set(providers.get(0));
        this.networkService = networkService;
        userAgent = "bisq-v2/" + version.toString();
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        shutdownStarted = true;
        return httpClient.map(BaseHttpClient::shutdown)
                .orElse(CompletableFuture.completedFuture(true));
    }

    public CompletableFuture<Tx> requestTx(String txId) {
        Provider provider = selectedProvider.get();
        BaseHttpClient httpClient = networkService.getHttpClient(provider.baseUrl, userAgent, provider.transportType);

        return CompletableFuture.supplyAsync(() -> {
            long ts = System.currentTimeMillis();
            String param = provider.getApiPath() + provider.getTxPath() + txId;
            try {
                String json = httpClient.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                log.info("Requesting tx from {} took {} ms", httpClient.getBaseUrl() + param, System.currentTimeMillis() - ts);
                return new ObjectMapper().readValue(json, Tx.class);
            } catch (IOException e) {
                if (!shutdownStarted) {
                    log.warn(ExceptionUtil.getMessageOrToString(e));
                }
                throw new RuntimeException(e);
            }
        }, POOL);
    }
}
