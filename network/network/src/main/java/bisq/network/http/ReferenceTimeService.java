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
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class ReferenceTimeService extends HttpRequestService<Void, Long> {
    // coinmarketcapTs and btcAverageTs are excluded as we use longer request time intervals for those.
    // coinbaseproTs has a timestamp of 0. independentreserveTs has a ts with 1 hour in future
    private static final List<String> TIMESTAMP_KEYS = List.of(
            "bitstampTs",
            "btcmarketsTs",
            "coingeckoTs",
            "binanceTs",
            "poloniexTs",
            "krakenTs",
            "bitflyerTs",
            "cryptoyaTs",
            "paribuTs",
            "bitfinexTs",
            "lunoTs",
            "yadioTs",
            "coinoneTs",
            "mercadobitcoinTs"
    );

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
        JsonNode jsonNode = JsonMapperProvider.get().readTree(json);

        List<Long> validTimestamps = TIMESTAMP_KEYS.stream()
                .mapToLong(key -> {
                    try {
                        return parseTimestamp(jsonNode, key);
                    } catch (JsonProcessingException ignore) {
                        return 0;
                    }
                })
                .filter(ts -> ts > 0)
                .sorted()
                .boxed()
                .toList();

        int size = validTimestamps.size();
        if (size == 0) {
            throw new RuntimeException("No valid timestamps found in response");
        }

        // Drop up to 3 outliers from each end, ensuring at least 1 remains
        int dropCount = Math.min(3, (size - 1) / 2);
        double averageTimestamp = validTimestamps.stream()
                .skip(dropCount)
                .limit(size - 2 * dropCount)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0d);

        long referenceTime = MathUtils.roundDoubleToLong(averageTimestamp);
        log.info("Reference time from {}: {} ({})", selectedProvider.get().getBaseUrl(), new Date(referenceTime), referenceTime);
        return referenceTime;
    }

    private Long parseTimestamp(JsonNode jsonNode, String key) throws JsonProcessingException {
        JsonNode timestampNode = jsonNode.get(key);
        if (timestampNode == null || timestampNode.isNull() || !timestampNode.isNumber()) {
            throw new RuntimeException("Response JSON missing or invalid field: '" + key + "'");
        }
        long referenceTime = timestampNode.asLong();
        log.debug("Timestamp from latest price request from {}: {} (epoche time in seconds: {})",
                key.replace("Ts", ""),
                new Date(referenceTime),
                referenceTime);
        return referenceTime;
    }

    @Override
    protected String getParam(HttpRequestUrlProvider provider, Void requestData) {
        return provider.getApiPath();
    }

    public CompletableFuture<Long> request() {
        return request(null);
    }
}
