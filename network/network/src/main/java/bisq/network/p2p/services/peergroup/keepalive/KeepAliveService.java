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

package bisq.network.p2p.services.peergroup.keepalive;

import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class KeepAliveService implements Node.Listener {
    private static final long TIMEOUT_SEC = 120;

    @Getter
    @ToString
    public static final class Config {
        private final long maxIdleTime;
        private final long interval;

        public Config(long maxIdleTime, long interval) {
            this.maxIdleTime = maxIdleTime;
            this.interval = interval;
        }

        public static Config from(com.typesafe.config.Config typesafeConfig) {
            return new Config(
                    SECONDS.toMillis(typesafeConfig.getLong("maxIdleTimeInSeconds")),
                    SECONDS.toMillis(typesafeConfig.getLong("intervalInSeconds"))
            );
        }
    }

    private final Node node;
    private final PeerGroupService peerGroupService;
    private final Config config;
    private final Map<String, KeepAliveHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private Optional<Scheduler> scheduler = Optional.empty();

    public KeepAliveService(Node node, PeerGroupService peerGroupService, Config config) {
        this.node = node;
        this.peerGroupService = peerGroupService;
        this.config = config;
        this.node.addListener(this);
    }

    public void initialize() {
        scheduler = Optional.of(Scheduler.run(this::sendPingIfRequired)
                .periodically(config.getInterval())
                .name("KeepAliveService.scheduler-" + node.getNodeInfo()));
    }

    private void sendPingIfRequired() {
        peerGroupService.getAllConnections()
                .filter(this::isRequired)
                .forEach(this::sendPing);
    }

    public void sendPing(Connection connection) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            log.info("requestHandlerMap contains {}. " +
                            "This is expected if the connection is still pending the response or the peer is not available " +
                            "but the timeout has not triggered an exception yet. We skip that request. Connection={}",
                    key, connection);
            return;
        }
        KeepAliveHandler handler = new KeepAliveHandler(node, connection);
        requestHandlerMap.put(key, handler);
        handler.request()
                .orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .whenComplete((__, throwable) -> requestHandlerMap.remove(key));
    }

    public void shutdown() {
        scheduler.ifPresent(Scheduler::stop);
        requestHandlerMap.values().forEach(KeepAliveHandler::dispose);
        requestHandlerMap.clear();
    }

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof Ping) {
            Ping ping = (Ping) envelopePayloadMessage;
            log.debug("Node {} received Ping with nonce {} from {}", node, ping.getNonce(), connection.getPeerAddress());
            NetworkService.NETWORK_IO_POOL.submit(() -> node.send(new Pong(ping.getNonce()), connection));
            log.debug("Node {} sent Pong with nonce {} to {}. Connection={}", node, ping.getNonce(), connection.getPeerAddress(), connection.getId());
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
        }
    }

    private boolean isRequired(Connection connection) {
        return System.currentTimeMillis() - connection.getConnectionMetrics().getLastUpdate().get() > config.getMaxIdleTime();
    }
}