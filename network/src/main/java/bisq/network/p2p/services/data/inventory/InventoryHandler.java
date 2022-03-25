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

import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Getter
@Slf4j
class InventoryHandler implements Connection.Listener {
    private final Node node;
    private final Connection connection;
    private final CompletableFuture<Inventory> future = new CompletableFuture<>();
    private final int nonce;
    private long ts;

    InventoryHandler(Node node, Connection connection) {
        this.node = node;
        this.connection = connection;

        nonce = new Random().nextInt();
        connection.addListener(this);
    }

    CompletableFuture<Inventory> request(DataFilter dataFilter) {
        log.info("Node {} send GetInventoryRequest to {} with dataFilter {} and nonce {}. Connection={}",
                node, connection.getPeerAddress(), dataFilter, nonce, connection.getId());
        ts = System.currentTimeMillis();
        supplyAsync(() -> node.send(new InventoryRequest(dataFilter, nonce), connection), NetworkService.NETWORK_IO_POOL)
                .whenComplete((c, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        dispose();
                    }
                });
        return future;
    }

    @Override
    public void onNetworkMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof InventoryResponse response) {
            if (response.requestNonce() == nonce) {
                Map<String, Integer> map = new HashMap<>();
                response.inventory().entries().stream()
                        .filter(e -> e instanceof AddAuthenticatedDataRequest)
                        .map(e -> (AddAuthenticatedDataRequest) e)
                        .map(AddAuthenticatedDataRequest::getAuthenticatedData)
                        .map(AuthenticatedData::getPayload)
                        .forEach(e -> {
                            String simpleName = e.getData().getClass().getSimpleName();
                            map.putIfAbsent(simpleName, 0);
                            map.put(simpleName, map.get(simpleName) + 1);
                        });
                log.info("Node {} received GetInventoryResponse from {} with inventory {} and nonce {}. Connection={}",
                        node, connection.getPeerAddress(), response.inventory(), response.requestNonce(), connection.getId());
                log.info("\n##########################################################################################\n" +
                        "## INVENTORY\n" +
                        "##########################################################################################\n" +
                        map.entrySet().stream().map(e ->  e.getValue() + " " + e.getKey() + "(s)").collect(Collectors.joining("\n")) +
                        "\n##########################################################################################");
                removeListeners();
                connection.getMetrics().addRtt(ts = System.currentTimeMillis() - ts);
                future.complete(response.inventory());
            } else {
                log.warn("Node {} received Pong from {} with invalid nonce {}. Request nonce was {}. Connection={}",
                        node, connection.getPeerAddress(), response.requestNonce(), nonce, connection.getId());
            }
        }
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