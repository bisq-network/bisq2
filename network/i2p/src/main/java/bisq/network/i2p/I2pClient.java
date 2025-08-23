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

import bisq.common.threading.ExecutorFactory;
import bisq.network.i2p.embedded.I2pEmbeddedRouter;
import bisq.security.keys.I2PKeyPair;
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PAppContext;
import net.i2p.client.I2PSessionException;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.Destination;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class I2pClient {
    private final int socketTimeout;
    private final SocketManagerByNodeId socketManagerByNodeId;
    private Optional<I2pEmbeddedRouter> embeddedI2pRouter = Optional.empty();
    private final ExecutorService routerInitExecutor;
    private volatile boolean isShutdownInProgress;

    public I2pClient(String dirPath, String i2cpHost,
                     int i2cpPort,
                     int socketTimeout,
                     int connectTimeout,
                     boolean isEmbeddedRouter) {
        this.socketTimeout = socketTimeout;
        socketManagerByNodeId = new SocketManagerByNodeId(i2cpHost, i2cpPort, socketTimeout, connectTimeout);
        this.routerInitExecutor = Executors.newSingleThreadExecutor();
        if (isEmbeddedRouter) {
            routerInitExecutor.submit(() -> {
                long start = System.currentTimeMillis();
                embeddedI2pRouter = Optional.of(I2pEmbeddedRouter.getInitializedI2pEmbeddedRouter());
                log.info("Embedded I2P router initialized asynchronously. Took {} ms.", System.currentTimeMillis() - start);
            });
        }
        I2PAppContext.getGlobalContext().logManager().setBaseLogfilename(dirPath + "/logs/i2p-@.log");
        log.info("I2P client created with i2cpHost={}, i2cpPort={}, socketTimeout={}", i2cpHost, i2cpPort, socketTimeout);
    }

    public void shutdown() {
        isShutdownInProgress = true;
        long ts = System.currentTimeMillis();
        socketManagerByNodeId.shutdown();
        ExecutorFactory.shutdownAndAwaitTermination(routerInitExecutor);
        embeddedI2pRouter.ifPresent(I2pEmbeddedRouter::shutdown);
        log.info("I2P shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
    }

    public ServerSocket getServerSocket(I2PKeyPair i2PKeyPair, String nodeId) throws IOException, I2PSessionException {
        if (isShutdownInProgress) {
            throw new IllegalStateException("I2P client is shutting down");
        }
        long ts = System.currentTimeMillis();
        I2PSocketManager manager = socketManagerByNodeId.createNewSocketManager(i2PKeyPair, nodeId);
        try {
            ServerSocket serverSocket = manager.getStandardServerSocket();
            log.info("Server socket for nodeId {} created. Took {} ms.", nodeId, System.currentTimeMillis() - ts);
            return serverSocket;
        } catch (IOException e) {
            socketManagerByNodeId.disposeSocketManager(nodeId);
            throw e;
        }
    }

    public Socket getSocket(Destination peersDestination, String nodeId) throws IOException {
        if (isShutdownInProgress) {
            throw new IllegalStateException("I2P client is shutting down");
        }
        long ts = System.currentTimeMillis();
        I2PSocketManager manager = socketManagerByNodeId.getSocketManager(nodeId);
        try {
            Socket socket = manager.connectToSocket(peersDestination, socketTimeout);
            log.info("Client socket for nodeId {} created. Took {} ms.", nodeId, System.currentTimeMillis() - ts);
            return socket;
        } catch (IOException e) {
            socketManagerByNodeId.disposeSocketManager(nodeId);
            throw e;
        }
    }

    // The lease can be still present for about 10 min after peer has been offline
    public boolean isLeaseFoundInNetDb(Destination peersDestination, String nodeId) {
        I2PSocketManager manager = socketManagerByNodeId.getSocketManager(nodeId);
        try {
            Destination resultDestination = manager.getSession().lookupDest(peersDestination.getHash());
            return resultDestination != null;
        } catch (I2PSessionException e) {
            throw new RuntimeException(e);
        }

/*        if (embeddedI2pRouter.isEmpty()) {
            log.warn("I2P router not yet initialized, cannot check peer status for: {}", peer);
            // todo how to support it with external router?
            // We need to return true, otherwise we would always send a mailbox msg in case of ext router
            return true;
        }
        // Tracks recent failed connection attempts your router has seen.
        return embeddedI2pRouter.get().isPeerOnline(destination);*/
    }
}
