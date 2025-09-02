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
import bisq.security.keys.I2PKeyPair;
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PAppContext;
import net.i2p.client.I2PSessionException;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.data.Destination;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
public class I2PClient {
    private final int socketTimeout;
    private final I2PSocketManagerByNodeId socketManagerByNodeId;
    private volatile boolean isShutdownInProgress;

    public I2PClient(Path clientDir,
                     String i2cpHost,
                     int i2cpPort,
                     int socketTimeout,
                     int connectTimeout) {
        this.socketTimeout = socketTimeout;
        socketManagerByNodeId = new I2PSocketManagerByNodeId(i2cpHost, i2cpPort, connectTimeout);

        I2PAppContext.getGlobalContext().logManager().setBaseLogfilename(clientDir+ "/log-@.log");
        log.info("I2P client created with i2cpHost={}, i2cpPort={}", i2cpHost, i2cpPort);
    }

    public CompletableFuture<Boolean> shutdown() {
        if (isShutdownInProgress) {
            return CompletableFuture.completedFuture(true);
        }
        isShutdownInProgress = true;
        ExecutorService executor = ExecutorFactory.newSingleThreadExecutor("I2PTransportService.shutdown");
        return CompletableFuture.supplyAsync(() -> {
                    long ts = System.currentTimeMillis();
                    socketManagerByNodeId.shutdown();
                    log.info("I2P shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
                    return true;
                }, executor)
                .whenComplete((result, throwable) -> ExecutorFactory.shutdownAndAwaitTermination(executor));
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
        Socket socket = manager.connectToSocket(peersDestination, socketTimeout);
        log.info("Client socket for nodeId {} created. Took {} ms.", nodeId, System.currentTimeMillis() - ts);
        return socket;
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
    }
}
