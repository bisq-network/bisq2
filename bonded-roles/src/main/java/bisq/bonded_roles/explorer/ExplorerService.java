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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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
            if (requestData instanceof TxRequestData txRequestData) {
                return provider.getApiPath() + explorerServiceProvider.getTxPath() + txRequestData.getTxId();
            } else if (requestData instanceof AddressRequestData addressRequestData) {
                return provider.getApiPath() + explorerServiceProvider.getAddressPath() + addressRequestData.getAddress();
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
            Set<HttpRequestUrlProvider> providers = parseProviders(typesafeConfig.getConfigList("providers"));
            Set<HttpRequestUrlProvider> fallbackProviders = parseProviders(typesafeConfig.getConfigList("fallbackProviders"));
            return new Config(timeoutInSeconds, providers, fallbackProviders);
        }

        public Config(long timeoutInSeconds, Set<HttpRequestUrlProvider> providers, Set<HttpRequestUrlProvider> fallbackProviders) {
            super(timeoutInSeconds, providers, fallbackProviders);
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static final class Provider extends HttpRequestUrlProvider {
        private final String txPath;
        private final String addressPath;

        public Provider(String baseUrl, String operator, TransportType transportType) {
            this(baseUrl, operator, "api/", "tx/", "address/", transportType);
        }

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
