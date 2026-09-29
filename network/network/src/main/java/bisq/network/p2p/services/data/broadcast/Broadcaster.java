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

package bisq.network.p2p.services.data.broadcast;

import bisq.common.util.CollectionUtil;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Broadcaster {
    // Timeout for the broadcast to all the target connections. We don't expect that parallel send on multiple connections
    // will take longer than 10 sec. for all connections.
    private static final long TIMEOUT = 10;

    private final Node node;

    public Broadcaster(Node node) {
        this.node = node;
    }

    public CompletableFuture<BroadcastResult> reBroadcast(BroadcastMessage broadcastMessage) {
        return doBroadcast(broadcastMessage, 0.75);
    }

    public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
        return doBroadcast(broadcastMessage, 1);
    }

    private CompletableFuture<BroadcastResult> doBroadcast(BroadcastMessage broadcastMessage,
                                                           double distributionFactor) {
        // NetworkService completes its initialization when the node of the first transport is ready. The node of
        // another transport can still be starting. It has no connections yet, so the broadcast reaches no peer on this
        // transport. It must not throw: the callers broadcast on all transports in one loop, and an exception would
        // stop the broadcast on the other transports.
        if (!node.isInitialized()) {
            log.info("Node {} is not initialized yet. We do not broadcast {}.",
                    node.getTransportType(), broadcastMessage.getClass().getSimpleName());
            return CompletableFuture.completedFuture(new BroadcastResult(0, 0, 0));
        }
        List<Connection> connections = getConnection(distributionFactor);
        if (connections.isEmpty()) {
            log.info("No connections available for broadcast from node {}.", node.getNodeInfo());
            return CompletableFuture.completedFuture(new BroadcastResult(0, 0, 0));
        } else {
            long ts = System.currentTimeMillis();
            List<CompletableFuture<Boolean>> sendFutures = connections.stream()
                    .map(connection -> {
                        log.debug("Broadcast {} to {}", broadcastMessage.getClass().getSimpleName(), connection.getPeerAddress());
                        return node.sendAsync(broadcastMessage, connection)
                                .handle((result, throwable) -> {
                                    if (throwable == null) {
                                        return true;
                                    } else {
                                        log.debug("Broadcast to {} failed.", connection.getPeerAddress(), throwable);
                                        return false;
                                    }
                                });
                    }).toList();
            return CompletableFutureUtils.allOf(sendFutures)
                    .thenApply(results -> {
                        int numSuccess = (int) results.stream().filter(success -> success).count();
                        int numFaults = (int) results.stream().filter(success -> !success).count();
                        long duration = System.currentTimeMillis() - ts;
                        return new BroadcastResult(numSuccess, numFaults, duration);
                    })
                    .orTimeout(TIMEOUT, TimeUnit.SECONDS);
        }
    }

    private List<Connection> getConnection(double distributionFactor) {
        List<Connection> allActiveConnections = node.getAllActiveConnections().toList();
        long numAllConnections = allActiveConnections.size();
        long broadcastTarget = Math.min(numAllConnections, Math.round(numAllConnections * distributionFactor));
        List<Connection> allShuffledConnections = CollectionUtil.toShuffledList(allActiveConnections);
        return allShuffledConnections.stream().limit(broadcastTarget).toList();
    }
}
