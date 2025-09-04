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

package bisq.network.i2p;

import bisq.security.keys.I2PKeyPair;
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PSessionException;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketOptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class I2PSocketManagerByNodeId {

    // Self-managing listener. Does remove itself at sessionDisconnected and call dispose.
    private static class DisconnectListener implements I2PSocketManager.DisconnectListener {
        private final String nodeId;
        private final I2PSocketManager manager;
        private final BiConsumer<String, I2PSocketManager> disposeHandler;

        public DisconnectListener(String nodeId,
                                  I2PSocketManager manager,
                                  BiConsumer<String, I2PSocketManager> disposeHandler) {
            this.nodeId = nodeId;
            this.manager = manager;
            this.disposeHandler = disposeHandler;
        }

        @Override
        public void sessionDisconnected() {
            log.warn("I2P socket manager for nodeId {} disconnected; disposing and removing from cache", nodeId);
            // Defensive: remove listener explicitly in case destroySocketManager()
            // does not clean up listeners itself.
            manager.removeDisconnectListener(this);
            disposeHandler.accept(nodeId, manager);
        }
    }

    private final String i2cpHost;
    private final int i2cpPort;
    private final int connectTimeout;
    private final Map<String, I2PSocketManager> socketManagerByNodeId = new ConcurrentHashMap<>();

    public I2PSocketManagerByNodeId(String i2cpHost,
                                    int i2cpPort,
                                    int connectTimeout) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.connectTimeout = connectTimeout;
    }

    synchronized void shutdown() {
        socketManagerByNodeId.values().forEach(I2PSocketManager::destroySocketManager);
        socketManagerByNodeId.clear();
    }

    synchronized I2PSocketManager createNewSocketManager(I2PKeyPair i2PKeyPair, String nodeId) {
        log.debug("createNewSocketManager {}", nodeId);
        checkArgument(!socketManagerByNodeId.containsKey(nodeId),
                "createNewSocketManager for nodeID %s must be called only once. ", nodeId);

        long ts = System.currentTimeMillis();
        log.info("Creating server socket manager for session {} on localPort {}", nodeId, i2cpPort);
        byte[] identityBytes = i2PKeyPair.getIdentityBytes();
        I2PSocketManager manager;
        try (ByteArrayInputStream identityBytesStream = new ByteArrayInputStream(identityBytes)) {
            manager = I2PSocketManagerFactory.createDisconnectedManager(identityBytesStream, i2cpHost, i2cpPort, getProperties());
        } catch (I2PSessionException | IOException e) {
            throw new RuntimeException(e);
        }
        manager.addDisconnectListener(new DisconnectListener(nodeId, manager, this::disposeSocketManager));
        applyOptions(manager);
        log.info("Server socket manager ready for session {}. Took {} ms.", nodeId, System.currentTimeMillis() - ts);
        socketManagerByNodeId.put(nodeId, manager);
        return manager;
    }

    I2PSocketManager getSocketManager(String nodeId) {
        log.debug("getSocketManager {}", nodeId);
        return checkNotNull(socketManagerByNodeId.get(nodeId),
                "socketManager must not be null as the client socket cannot be called before we have created the server socket. nodeId=" + nodeId);
    }

    synchronized void disposeSocketManager(String nodeId) {
        Optional.ofNullable(socketManagerByNodeId.get(nodeId))
                .ifPresent(manager -> {
                    manager.destroySocketManager();
                    socketManagerByNodeId.remove(nodeId);
                });
    }

    synchronized void disposeSocketManager(String nodeId, I2PSocketManager expected) {
        I2PSocketManager current = socketManagerByNodeId.get(nodeId);
        // Use object identity to be sure it's the same reference.
        if (current == expected) {
            current.destroySocketManager();
            socketManagerByNodeId.remove(nodeId);
        } else {
            // Old manager disconnected after replacement; only destroy the old one.
            expected.destroySocketManager();
            log.info("Ignored disconnect from stale manager for nodeId {}", nodeId);
        }
    }

    private void applyOptions(I2PSocketManager manager) {
        I2PSocketOptions options = manager.buildOptions();
        // 120 sec; DEFAULT_CONNECT_TIMEOUT = 60 * 1000
        // Seems to be the same as `manager.setAcceptTimeout(connectTimeout);`
        options.setConnectTimeout(connectTimeout);

        options.setReadTimeout(TimeUnit.MINUTES.toMillis(3)); // default -1 -> wait forever
        options.setWriteTimeout(TimeUnit.MINUTES.toMillis(3)); // default -1 -> wait forever
        //options.setMaxBufferSize(256 * 1024); // DEFAULT_BUFFER_SIZE = 1024*64;
        manager.setDefaultOptions(options);
    }

    private static Properties getProperties() {
        Properties props = new Properties();
        // Number of parallel tunnels; default DEFAULT_TUNNEL_QUANTITY = 3
        props.setProperty("inbound.quantity", "3");
        props.setProperty("outbound.quantity", "3");
        props.setProperty("i2cp.closeOnIdle", "false");
        props.setProperty("i2cp.reduceOnIdle", "false");
        return props;
    }
}
