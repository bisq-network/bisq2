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
import bisq.common.threading.ThreadName;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
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
    private long requestTs;

    InventoryHandler(Node node, Connection connection) {
        this.node = node;
        this.connection = connection;

        nonce = new Random().nextInt();
        connection.addListener(this);
    }

    CompletableFuture<Inventory> request(InventoryFilter inventoryFilter) {
        requestTs = System.currentTimeMillis();
        log.info("Send InventoryRequest to {} with {}", connection.getPeerAddress(), inventoryFilter.getDetails());
        InventoryRequest inventoryRequest = new InventoryRequest(inventoryFilter, nonce);
        runAsync(() -> {
            ThreadName.set(this, "request");
            node.send(inventoryRequest, connection);
        }, NetworkService.NETWORK_IO_POOL)
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
        if (envelopePayloadMessage instanceof InventoryResponse response) {
            if (response.getRequestNonce() == nonce) {
                printReceivedInventory(response);
                removeListeners();
                future.complete(response.getInventory());
            } else {
                log.warn("Received InventoryResponse from {} with invalid nonce {}. Request nonce was {}. Peer address={}",
                        connection.getPeerAddress(), response.getRequestNonce(), nonce,
                        connection.getPeerAddress().getFullAddress());
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
                    if (dataRequest instanceof AddAuthenticatedDataRequest addRequest) {
                        payloadName = addRequest.getDistributedData().getClass().getSimpleName();
                    } else if (dataRequest instanceof RemoveAuthenticatedDataRequest removeRequest) {
                        payloadName = removeRequest.getClassName();
                    } else if (dataRequest instanceof RefreshAuthenticatedDataRequest request) {
                        payloadName = request.getClassName();
                    } else if (dataRequest instanceof AddAppendOnlyDataRequest addRequest) {
                        payloadName = addRequest.getAppendOnlyData().getClass().getSimpleName();
                    } else if (dataRequest instanceof AddMailboxRequest addRequest) {
                        payloadName = addRequest.getMailboxSequentialData().getMailboxData().getClassName();
                    } else if (dataRequest instanceof RemoveMailboxRequest removeRequest) {
                        payloadName = removeRequest.getClassName();
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
        String maxSizeReached = inventory.isMaxSizeReached()
                ? "Still missing data. Response got truncated because max size was reached"
                : "All data received from peer";
        String size = ByteUnit.BYTE.toKB((double) inventory.getCachedSerializedSize().orElse(0)) + " KB";
        String passed = MathUtils.roundDouble((System.currentTimeMillis() - requestTs) / 1000d, 2) + " sec.";
        log.info("\n##########################################################################################\nReceived {} of inventory data from: {} after {}; \n{}\n##########################################################################################\n{}\n##########################################################################################", size, connection.getPeerAddress().getFullAddress(), passed, maxSizeReached, report);
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