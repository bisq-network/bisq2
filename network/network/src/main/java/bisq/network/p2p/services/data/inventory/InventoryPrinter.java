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

package bisq.network.p2p.services.data.inventory;

import bisq.common.data.ByteUnit;
import bisq.common.util.MathUtils;
import bisq.network.p2p.node.Connection;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class InventoryPrinter {
    static void print(InventoryResponse response,
                      Connection connection,
                      Map<String, Long> requestTimestampByConnectionId) {
        Long requestTsValue = requestTimestampByConnectionId.get(connection.getId());
        if (requestTsValue == null) {
            log.warn("Map entry for {} not present in requestTimestampByConnectionId", connection.getId());
            return;
        }
        long requestTs = requestTsValue;
        Inventory inventory = response.getInventory();
        String report = createReport(inventory);
        int numEntries = inventory.getEntries().size();
        String maxSizeReached = inventory.isMaxSizeReached()
                ? "Still missing data. Response got truncated because max size was reached"
                : numEntries == 0 ? "No data received from peer." : "All data received from peer. Num entries: " + numEntries;
        String size = ByteUnit.BYTE.toKB((double) inventory.getCachedSerializedSize().orElse(0)) + " KB";
        String passed = MathUtils.roundDouble((System.currentTimeMillis() - requestTs) / 1000d, 2) + " sec.";
        log.info("\n##########################################################################################\n" +
                        "Received {} of inventory data from: {} after {}; \n{}" +
                        "\n##########################################################################################\n{}" +
                        "\n##########################################################################################",
                size, connection.getPeerAddress(), passed, maxSizeReached, report);
    }

    // The number of requests per request type and class name of the data
    @VisibleForTesting
    static String createReport(Inventory inventory) {
        Map<String, Map<String, AtomicInteger>> dataRequestMap = new HashMap<>();
        inventory.getEntries()
                .forEach(dataRequest -> {
                    String dataRequestName = dataRequest.getClass().getSimpleName();
                    String payloadName = dataRequest.getClassName();
                    dataRequestMap.putIfAbsent(dataRequestName, new HashMap<>());
                    Map<String, AtomicInteger> payloadMap = dataRequestMap.get(dataRequestName);
                    dataRequestMap.put(dataRequestName, payloadMap);
                    String payloadKey = dataRequestName.equals(payloadName) ?
                            dataRequestName :
                            dataRequestName + "." + payloadName;

                    payloadMap.putIfAbsent(payloadKey, new AtomicInteger());
                    AtomicInteger counter = payloadMap.get(payloadKey);
                    counter.incrementAndGet();
                });
        String report = dataRequestMap.values().stream()
                .map(payloadMap -> payloadMap.entrySet().stream()
                        .map(entry -> String.format("%4d item(s) of %s", entry.getValue().get(), entry.getKey()))
                        .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n"));
        return report.isEmpty() ? "No items received" : report;
    }
}
