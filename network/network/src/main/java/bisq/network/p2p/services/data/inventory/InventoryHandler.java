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

import bisq.common.util.ByteUnit;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.runAsync;

@Getter
@Slf4j
class InventoryHandler implements Connection.Listener {
    private final Node node;
    private final Connection connection;
    private final CompletableFuture<Inventory> future = new CompletableFuture<>();
    private final int nonce;

    InventoryHandler(Node node, Connection connection) {
        this.node = node;
        this.connection = connection;

        nonce = new Random().nextInt();
        connection.addListener(this);
    }

    CompletableFuture<Inventory> request(InventoryFilter inventoryFilter) {
        log.info("Send InventoryRequest to {} with {}", connection.getPeerAddress(), inventoryFilter.getDetails());
        InventoryRequest inventoryRequest = new InventoryRequest(inventoryFilter, nonce);
        runAsync(() -> node.send(inventoryRequest, connection), NetworkService.NETWORK_IO_POOL)
                .whenComplete((connection, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        removeListeners();
                    }
                });
        return future;
    }

    @Override
    public void onNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof InventoryResponse) {
            InventoryResponse response = (InventoryResponse) envelopePayloadMessage;
            if (response.getRequestNonce() == nonce) {
                printReceivedInventory(response);
                removeListeners();
                future.complete(response.getInventory());
            } else {
                log.warn("{} received InventoryResponse from {} with invalid nonce {}. Request nonce was {}. Connection={}",
                        node, connection.getPeerAddress(), response.getRequestNonce(), nonce, connection.getId());
            }
        }
    }

    private void printReceivedInventory(InventoryResponse response) {
        Map<String, Map<String, AtomicInteger>> dataRequestMap = new HashMap<>();
        Inventory inventory = response.getInventory();
        inventory.getEntries()
                .forEach(dataRequest -> {
                    String dataRequestName = dataRequest.getClass().getSimpleName();
                    String payloadName = dataRequest.getClass().getSimpleName();
                    if (dataRequest instanceof AddAuthenticatedDataRequest) {
                        AddAuthenticatedDataRequest addRequest = (AddAuthenticatedDataRequest) dataRequest;
                        payloadName = addRequest.getAuthenticatedSequentialData().getAuthenticatedData().getDistributedData().getClass().getSimpleName();
                    } else if (dataRequest instanceof AddAppendOnlyDataRequest) {
                        AddAppendOnlyDataRequest addRequest = (AddAppendOnlyDataRequest) dataRequest;
                        payloadName = addRequest.getAppendOnlyData().getClass().getSimpleName();
                    }
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

        if (report.isEmpty()) {
            report = "No items received";
        }
        String maxSizeReached = inventory.isMaxSizeReached() ? "; \nResponse got truncated because max size was reached" : "";
        String size = ByteUnit.BYTE.toKB((double) inventory.getSerializedSize().orElse(0)) + " KB";
        log.info("\n##########################################################################################\n" +
                "Received " + size + " of inventory data from: " + connection.getPeerAddress().getFullAddress() +
                maxSizeReached +
                "\n##########################################################################################\n" +
                report +
                "\n##########################################################################################");
    }

    @Override
    public void onConnectionClosed(CloseReason closeReason) {
        dispose();
    }

    void dispose() {
        removeListeners();
        future.cancel(true);
    }

    private void removeListeners() {
        connection.removeListener(this);
    }
}