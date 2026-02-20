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
import bisq.common.json.JsonMapperProvider;
import bisq.common.network.TransportType;
import bisq.common.threading.ExecutorFactory;
import bisq.network.NetworkService;
import bisq.network.http.HttpRequestService;
import bisq.network.http.HttpRequestServiceConfig;
import bisq.network.http.HttpRequestUrlProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
public class ExplorerService extends HttpRequestService<ExplorerService.RequestData, Tx> {
    private static ExecutorService getExecutorService() {
        return ExecutorFactory.newCachedThreadPool(ExplorerService.class.getSimpleName(),
                1,
                5,
                60);
    }

    public ExplorerService(ExplorerService.Config conf, NetworkService networkService) {
        super(conf,
                networkService,
                getExecutorService());
    }

    @Override
    protected Tx parseResult(String json) throws JsonProcessingException {
        return JsonMapperProvider.get().readValue(json, Tx.class);
    }

    @Override
    protected String getParam(HttpRequestUrlProvider provider, RequestData requestData) {
        if (provider instanceof Provider explorerServiceProvider) {
            String apiPath = provider.getApiPath();
            if (requestData instanceof TxRequestData txRequestData) {
                String txPath = explorerServiceProvider.getTxPath();
                String txId = txRequestData.getTxId();
                return apiPath + "/" + txPath + "/" + txId;
            } else if (requestData instanceof AddressRequestData addressRequestData) {
                String addressPath = explorerServiceProvider.getAddressPath();
                String address = addressRequestData.getAddress();
                return apiPath + "/" + addressPath + "/" + address;
            } else {
                throw new IllegalArgumentException("requestData of unsupported type. requestData = " + requestData);
            }
        } else {
            throw new IllegalArgumentException("Provider must be an instance of ExplorerService.Provider");
        }
    }

    public CompletableFuture<Tx> requestTx(String txId) {
        return request(new TxRequestData(txId));
    }

    public Optional<Provider> getExplorerServiceProvider() {
        if (getSelectedProvider().get() instanceof ExplorerService.Provider explorerServiceProvider) {
            return Optional.of(explorerServiceProvider);
        } else {
            log.warn("SelectedProvider is null or not of type ExplorerService.Provider");
            return Optional.empty();
        }
    }

    @Getter
    @ToString
    public static final class Config extends HttpRequestServiceConfig {
        public static Config from(com.typesafe.config.Config typesafeConfig) {
            long timeoutInSeconds = typesafeConfig.getLong("timeoutInSeconds");
            Set<Provider> providers = parseExplorerProviders(typesafeConfig.getConfigList("providers"));
            Set<Provider> fallbackProviders = parseExplorerProviders(typesafeConfig.getConfigList("fallbackProviders"));
            return new Config(timeoutInSeconds, providers, fallbackProviders);
        }

        public Config(long timeoutInSeconds, Set<Provider> providers, Set<Provider> fallbackProviders) {
            super(timeoutInSeconds, providers, fallbackProviders);
        }

        private static Set<Provider> parseExplorerProviders(List<? extends com.typesafe.config.Config> configList) {
            return configList.stream()
                    .map(config -> {
                        String url = config.getString("url");
                        String operator = config.getString("operator");
                        String apiPath = config.getString("apiPath");
                        String txPath = config.getString("txPath");
                        String addressPath = config.getString("addressPath");
                        TransportType transportType = getTransportTypeFromUrl(url);
                        return new Provider(url, operator, apiPath, txPath, addressPath, transportType);
                    })
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static final class Provider extends HttpRequestUrlProvider {
        private final String txPath;
        private final String addressPath;

        public Provider(String baseUrl,
                        String operator,
                        String apiPath,
                        String txPath,
                        String addressPath,
                        TransportType transportType) {
            super(baseUrl, operator, apiPath, transportType);
            this.txPath = txPath;
            this.addressPath = addressPath;
        }
    }

    public sealed interface RequestData permits TxRequestData, AddressRequestData {
    }

    @EqualsAndHashCode
    @ToString
    @Getter
    public static final class TxRequestData implements RequestData {
        private final String txId;

        public TxRequestData(String txId) {
            this.txId = txId;
        }
    }

    @EqualsAndHashCode
    @ToString
    @Getter
    public static final class AddressRequestData implements RequestData {
        private final String address;

        public AddressRequestData(String address) {
            this.address = address;
        }
    }
}
