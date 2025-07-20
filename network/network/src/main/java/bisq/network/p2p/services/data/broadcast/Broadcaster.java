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

import bisq.common.threading.ThreadName;
import bisq.common.util.CollectionUtil;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodeNotInitializedException;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Broadcaster {
    private static final long BROADCAST_TIMEOUT = 90;
    private static final long RE_BROADCAST_DELAY_MS = 100;

    private final Node node;
    private final RetryPolicy<BroadcastResult> retryPolicy;

    public Broadcaster(Node node) {
        this.node = node;

        retryPolicy = RetryPolicy.<BroadcastResult>builder()
                .handle(NodeNotInitializedException.class)
                .withBackoff(Duration.ofSeconds(3), Duration.ofSeconds(30))
                .withJitter(0.25)
                .withMaxDuration(Duration.ofMinutes(5)).withMaxRetries(10)
                .onRetry(e -> log.info("Retry. AttemptCount={}.", e.getAttemptCount()))
                .onRetriesExceeded(e -> log.warn("Max retries exceeded."))
                .onSuccess(e -> log.debug("Succeeded."))
                .build();
    }

    public CompletableFuture<BroadcastResult> reBroadcast(BroadcastMessage broadcastMessage) {
        return broadcast(
                broadcastMessage,
                0.75,
                CompletableFuture.delayedExecutor(
                        RE_BROADCAST_DELAY_MS,
                        TimeUnit.MILLISECONDS,
                        NetworkService.NETWORK_IO_LOW_PRIORITY_POOL
                )
        );
    }

    public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage) {
        return broadcast(broadcastMessage, 1);
    }

    public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage, double distributionFactor) {
        return broadcast(broadcastMessage, distributionFactor, NetworkService.NETWORK_IO_POOL);
    }

    public CompletableFuture<BroadcastResult> broadcast(BroadcastMessage broadcastMessage,
                                                        double distributionFactor,
                                                        Executor executor) {
        // Failsafe is only used to retry when node is not initialized yet.
        // if any other error occurs, we don't want to retry the broadcast.
        return Failsafe
                .with(retryPolicy)
                .with(executor)
                .getAsync(() -> doBroadcast(broadcastMessage, distributionFactor))
                .orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
    }

    private BroadcastResult doBroadcast(BroadcastMessage broadcastMessage,
                                        double distributionFactor) {
        ThreadName.set(this, "broadcast");
        if (!node.isInitialized()) {
            throw new NodeNotInitializedException("Node not initialized. node=" + node.getNetworkId() +
                    "; transportType=" + node.getTransportType());
        }

        long ts = System.currentTimeMillis();
        AtomicInteger numSuccess = new AtomicInteger(0);
        AtomicInteger numFaults = new AtomicInteger(0);
        long numConnections = node.getAllActiveConnections().count();
        long numBroadcasts = Math.min(numConnections, Math.round(numConnections * distributionFactor));
        log.debug("Broadcast {} to {} out of {} peers. distributionFactor={}",
                broadcastMessage.getClass().getSimpleName(), numBroadcasts, numConnections, distributionFactor);
        List<Connection> allConnections = CollectionUtil.toShuffledList(node.getAllActiveConnections());

        allConnections.stream()
                .limit(numBroadcasts)
                .forEach(connection -> {
                    log.debug("{} broadcast {} to {}", node, broadcastMessage.getClass().getSimpleName(), connection.getPeerAddress());
                    try {
                        node.send(broadcastMessage, connection);
                        numSuccess.incrementAndGet();
                    } catch (Exception exception) {
                        numFaults.incrementAndGet();
                    }
                });
        return new BroadcastResult(
                numSuccess.get(),
                numFaults.get(),
                System.currentTimeMillis() - ts
        );
    }
}
