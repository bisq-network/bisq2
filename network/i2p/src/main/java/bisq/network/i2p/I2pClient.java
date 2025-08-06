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

import bisq.common.file.FileUtils;
import bisq.network.i2p.util.I2PNameResolver;
import bisq.security.keys.I2PKeyPair;
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PAppContext;
import net.i2p.client.I2PSession;
import net.i2p.client.I2PSessionException;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketOptions;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class I2pClient {
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 7656;
    public static final long DEFAULT_SOCKET_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    private static final Map<String, I2pClient> I2P_CLIENT_BY_APP = new ConcurrentHashMap<>();
    private static final ExecutorService routerInitExecutor = Executors.newSingleThreadExecutor();

    private final boolean embeddedRouter;
    private I2pEmbeddedRouter i2pRouter;
    private final long socketTimeout;
    private final String dirPath;
    private I2PSession i2pSession;
    private I2PSocketManager i2PSocketManager;
    private final Map<String, I2PSocketManager> sessionMap = new ConcurrentHashMap<>();


    public static I2pClient getI2pClient(String dirPath,
                                         String host,
                                         int port,
                                         long socketTimeout,
                                         boolean isEmbeddedRouter) {
        synchronized (I2P_CLIENT_BY_APP) {
            return I2P_CLIENT_BY_APP.computeIfAbsent(dirPath,
                    k -> new I2pClient(dirPath, host, port, socketTimeout, isEmbeddedRouter));
        }
    }

    private I2pClient(String dirPath, String host, int port, long socketTimeout, boolean isEmbeddedRouter) {
        this.embeddedRouter = isEmbeddedRouter;
        this.socketTimeout = socketTimeout;
        this.dirPath = dirPath;

        if (isEmbeddedRouter) {
            routerInitExecutor.submit(() -> {
                long start = System.currentTimeMillis();
                this.i2pRouter = I2pEmbeddedRouter.getInitializedI2pEmbeddedRouter();
                log.info("Embedded I2P router initialized asynchronously. Took {} ms.", System.currentTimeMillis() - start);
            });
        }

        configureI2pLogging();
        log.info("I2P client created with dirPath={}, host={}, port={}, socketTimeout={}", dirPath, host, port, socketTimeout);
    }

    public Socket getSocket(String peer, String sessionId) throws IOException {
        try {
            long ts = System.currentTimeMillis();
            log.debug("Creating session {}", sessionId);

            Destination destination = getDestinationFor(peer);
            I2PSocketManager manager = maybeCreateClientSession(sessionId);
            i2PSocketManager = manager;
            I2PSession i2PSession = manager.getSession();
            i2PSession.connect();
            Socket socket = manager.connectToSocket(destination, Math.toIntExact(socketTimeout));
            log.info("Client socket for session {} created. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
            return socket;
        } catch (IOException e) {
            handleIOException(e, sessionId);
            throw e;
        } catch (I2PSessionException e) {
            throw new RuntimeException(e);
        }
    }

    public ServerSocket getServerSocket(I2PKeyPair i2PKeyPair,
                                        String sessionId,
                                        String host,
                                        int port) throws IOException {
        I2PSocketManager i2PSocketManager = maybeCreateServerSession(i2PKeyPair, sessionId, host, port);
        return i2PSocketManager.getStandardServerSocket();
    }

    public void shutdown() {
        long ts = System.currentTimeMillis();
        sessionMap.values().forEach(I2PSocketManager::destroySocketManager);
        sessionMap.clear();

        if (embeddedRouter && i2pRouter != null) {
            i2pRouter.shutdown();
        }

        routerInitExecutor.shutdownNow();
        log.info("I2P shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
    }

    private String getFileName(String sessionId) throws IOException {
        FileUtils.makeDirs(dirPath);
        return dirPath + File.separator + sessionId;
    }

    private I2PSocketManager maybeCreateClientSession(String sessionId) {
        return sessionMap.computeIfAbsent(sessionId, k -> I2PSocketManagerFactory.createManager());
    }

    private I2PSocketManager maybeCreateServerSession(
            I2PKeyPair i2PKeyPair,
            String sessionId,
            String host,
            int port
    ) throws IOException {
        // If we already have a manager for this session, return it immediately:
        if (sessionMap.containsKey(sessionId)) {
            return sessionMap.get(sessionId);
        }

        synchronized (sessionId.intern()) {
            if (sessionMap.containsKey(sessionId)) {
                return sessionMap.get(sessionId);
            }

            long ts = System.currentTimeMillis();
            log.info("Creating server socket manager for session {} using port {}", sessionId, port);

            //
            // Instead of reading a .priv_key file from disk, we already have a Destination
            // inside i2PKeyPair. We can get its raw bytes with destination.toByteArray().
            //
            byte[] rawDestBytes = i2PKeyPair.getDestination();

            // Wrap those bytes in a ByteArrayInputStream and pass it to createDisconnectedManager:
            I2PSocketManager manager;
            try (ByteArrayInputStream privKeyStream = new ByteArrayInputStream(rawDestBytes)) {
                manager = I2PSocketManagerFactory.createDisconnectedManager(
                        privKeyStream,
                        host,
                        port,
                        null
                );
            } catch (I2PSessionException e) {
                throw new RuntimeException(e);
            }

            // Apply any socket options you need:
            I2PSocketOptions options = manager.getDefaultOptions();
            options.setLocalPort(port);
            options.setConnectTimeout(Math.toIntExact(socketTimeout));
            manager.setDefaultOptions(options);

            i2pSession = manager.getSession();
//            try {
//                manager.getSession().connect();
//            } catch (I2PSessionException e) {
//                throw new IOException("Cannot build I2P tunnels for server", e);
//            }
            // Cache and return
            sessionMap.put(sessionId, manager);
            log.info("Server socket manager ready for session {}. Took {} ms.",
                    sessionId, System.currentTimeMillis() - ts);
            return manager;
        }
    }


    private Destination getDestinationFor(String peer) throws IOException {
        try {
            if (peer.endsWith(".b32.i2p") || peer.endsWith(".i2p")) {
                return I2PNameResolver.getDestinationFor(peer);
            }
            return new Destination(peer);
        } catch (DataFormatException e) {
            log.warn("Invalid destination format: {}", peer);
            throw new IOException("Invalid destination format", e);
        }
    }

    private void configureI2pLogging() {
        String baseLogFilename = dirPath + "/logs/i2p-@.log";
        I2PAppContext.getGlobalContext().logManager().setBaseLogfilename(baseLogFilename);
    }

    protected void handleIOException(IOException e, String sessionId) {
        log.error("IO Exception for session {}: {}", sessionId, e.getMessage(), e);
        I2PSocketManager manager = sessionMap.get(sessionId);
        if (manager != null && manager.listSockets().isEmpty()) {
            manager.destroySocketManager();
            sessionMap.remove(sessionId);
        }
    }

    public boolean isPeerOnline(String peer) {
        Destination destination = null;
        try {
            destination = getDestinationFor(peer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return destination != null & i2pRouter.isPeerOnline(destination);
    }

    public Destination lookupDest(String address) {
        Destination destination = null;
        try {
            destination = i2pSession.lookupDest(address);
        } catch (I2PSessionException e) {
            log.error("Error during destination lookup: {}", e.getMessage(), e);
        }
        return destination;
    }
}
