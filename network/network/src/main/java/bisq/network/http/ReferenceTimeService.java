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

import bisq.common.json.JsonMapperProvider;
import bisq.common.threading.ExecutorFactory;
import bisq.network.NetworkService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ReferenceTimeService extends HttpRequestService<Void, Long> {
    private static ExecutorService getExecutorService() {
        return ExecutorFactory.newCachedThreadPool(ReferenceTimeService.class.getSimpleName(),
                1,
                4,
                60);
    }

    public ReferenceTimeService(HttpRequestServiceConfig conf, NetworkService networkService) {
        super(conf,
                networkService,
                getExecutorService());
    }

    @Override
    protected Long parseResult(String json) throws JsonProcessingException {
        JsonNode timeNode = JsonMapperProvider.get().readTree(json).get("time");
        if (timeNode == null || timeNode.isNull() || !timeNode.isNumber()) {
            throw new RuntimeException("Response JSON missing 'time' field");
        }
        return timeNode.asLong() * 1000;
    }

    @Override
    protected String getParam(HttpRequestUrlProvider provider, Void requestData) {
        return provider.getApiPath();
    }

    public CompletableFuture<Long> request() {
        return request(null);
    }
}
