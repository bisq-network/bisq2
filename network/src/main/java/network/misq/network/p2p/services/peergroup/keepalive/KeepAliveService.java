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

package network.misq.network.p2p.services.peergroup.keepalive;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.timer.Scheduler;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.services.peergroup.PeerGroup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KeepAliveService implements Node.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    public static record Config(long maxIdleTime, long interval) {
    }

    private final Node node;
    private final PeerGroup peerGroup;
    private final Config config;
    private final Map<String, KeepAliveHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private Scheduler scheduler;

    public KeepAliveService(Node node, PeerGroup peerGroup, Config config) {
        this.node = node;
        this.peerGroup = peerGroup;
        this.config = config;
        this.node.addListener(this);
    }

    public void initialize() {
        scheduler = Scheduler.run(this::sendPingIfRequired).periodically(config.interval());
    }

    private void sendPingIfRequired() {
        peerGroup.getAllConnectionsAsStream()
                .filter(this::isRequired)
                .forEach(this::sendPing);
    }

    private void sendPing(Connection connection) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            log.warn("requestHandlerMap contains already {}. " +
                    "We dispose the existing handler and start a new one. Connection={}", connection.getPeerAddress(), connection);
            requestHandlerMap.get(key).dispose();
        }
        KeepAliveHandler handler = new KeepAliveHandler(node, connection);
        requestHandlerMap.put(key, handler);
        handler.request()
                .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                .whenComplete((__, throwable) -> requestHandlerMap.remove(key));
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        requestHandlerMap.values().forEach(KeepAliveHandler::dispose);
        requestHandlerMap.clear();
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof Ping ping) {
            log.debug("Node {} received Ping with nonce {} from {}", node, ping.nonce(), connection.getPeerAddress());
            node.send(new Pong(ping.nonce()), connection);
            log.debug("Node {} sent Pong with nonce {} to {}. Connection={}", node, ping.nonce(), connection.getPeerAddress(), connection.getId());
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
        }
    }

    private boolean isRequired(Connection connection) {
        return System.currentTimeMillis() - connection.getMetrics().getLastUpdate().get() > config.maxIdleTime();
    }

}