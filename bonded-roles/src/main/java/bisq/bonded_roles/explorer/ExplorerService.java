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
import bisq.common.json.JsonMapperProvider;
import bisq.common.threading.ExecutorFactory;
import bisq.network.HttpRequestBaseService;
import bisq.network.NetworkService;
import bisq.network.http.BaseHttpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;


// TODO We should support same registration model via oracle node as used with other nodes

@Slf4j
public class ExplorerService extends HttpRequestBaseService {
    private static final ExecutorService POOL = ExecutorFactory.newCachedThreadPool("ExplorerService", 1, 5, 60);


    public ExplorerService(Config conf, NetworkService networkService) {
        super("api/tx/", conf, networkService, POOL);
    }

    public CompletableFuture<Tx> requestTx(String txId) {
        return executeWithRetry(() -> makeRequest(provider -> {
            long ts = System.currentTimeMillis();
            String param = provider.getApiPath() + txId;
            log.info("Request tx with ID {} from {}", txId, provider.getBaseUrl() + "/" + param);
            
            BaseHttpClient client = networkService.getHttpClient(provider.getBaseUrl(), userAgent, provider.getTransportType());
            try {
                String json = client.get(param, Optional.of(new Pair<>("User-Agent", userAgent)));
                log.info("Received tx lookup response from {}/{} after {} ms", provider.getBaseUrl(), param, System.currentTimeMillis() - ts);
                
                return CompletableFuture.completedFuture(JsonMapperProvider.get().readValue(json, Tx.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
