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

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SocketManagerByNodeId {
    private final Map<String, I2PSocketManager> socketManagerByNodeId = new ConcurrentHashMap<>();
    private final String i2cpHost;
    private final int i2cpPort;
    private final int socketTimeout;

    public SocketManagerByNodeId(String i2cpHost,
                                 int i2cpPort,
                                 int socketTimeout) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.socketTimeout = socketTimeout;
    }

    synchronized void shutdown() {
        socketManagerByNodeId.values().forEach(I2PSocketManager::destroySocketManager);
        socketManagerByNodeId.clear();
    }

    synchronized I2PSocketManager getSocketManager(I2PKeyPair i2PKeyPair, String nodeId) {
        return socketManagerByNodeId.computeIfAbsent(nodeId, key -> {
            long ts = System.currentTimeMillis();
            log.info("Creating server socket manager for session {} on localPort {}", nodeId, i2cpPort);
            byte[] identityBytes = i2PKeyPair.getIdentityBytes();
            I2PSocketManager manager;
            try (ByteArrayInputStream identityBytesStream = new ByteArrayInputStream(identityBytes)) {
                manager = I2PSocketManagerFactory.createDisconnectedManager(identityBytesStream, i2cpHost, i2cpPort, getProperties());
            } catch (I2PSessionException | IOException e) {
                throw new RuntimeException(e);
            }
            manager.setAcceptTimeout(120_000);
            applyOptions(manager);
            manager.addDisconnectListener(() -> {
                log.warn("I2P socket disconnected");
                //todo
            });
            log.info("Server socket manager ready for session {}. Took {} ms.", nodeId, System.currentTimeMillis() - ts);
            return manager;
        });
    }

    I2PSocketManager getSocketManager(String nodeId) {
        return checkNotNull(socketManagerByNodeId.get(nodeId),
                "socketManager must not be null as the client socket cannot be called before we have created the server socket.");
    }

    synchronized void disposeSocketManager(String nodeId) {
        Optional.ofNullable(socketManagerByNodeId.get(nodeId))
                .ifPresent(manager -> {
                    manager.destroySocketManager();
                    socketManagerByNodeId.remove(nodeId);
                });
    }

    private void applyOptions(I2PSocketManager manager) {
        I2PSocketOptions options = manager.buildOptions();
        options.setConnectTimeout(socketTimeout); // 120 sec; DEFAULT_CONNECT_TIMEOUT = 60 * 1000
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
