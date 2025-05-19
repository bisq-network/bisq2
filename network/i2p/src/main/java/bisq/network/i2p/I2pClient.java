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
import lombok.extern.slf4j.Slf4j;
import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketOptions;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKeyFile;

import java.io.File;
import java.io.FileInputStream;
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

    private final Map<String, I2PSocketManager> sessionMap = new ConcurrentHashMap<>();


    public static I2pClient getI2pClient(String dirPath, String host, int port, long socketTimeout, boolean isEmbeddedRouter) {
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
            Socket socket = manager.connectToSocket(destination, Math.toIntExact(socketTimeout));

            log.info("Client socket for session {} created. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
            return socket;
        } catch (IOException e) {
            handleIOException(e, sessionId);
            throw e;
        }
    }

    public ServerSocket getServerSocket(String sessionId, String host, int port) throws IOException {
        return maybeCreateServerSession(sessionId, host, port).getStandardServerSocket();
    }

    public String getMyDestination(String sessionId) throws IOException {
        return FileUtils.readAsString(getFileName(sessionId) + ".destination");
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

    private I2PSocketManager maybeCreateServerSession(String sessionId, String host, int port) throws IOException {
        if (sessionMap.containsKey(sessionId)) {
            return sessionMap.get(sessionId);
        }

        synchronized (sessionId.intern()) {
            if (sessionMap.containsKey(sessionId)) {
                return sessionMap.get(sessionId);
            }

            long ts = System.currentTimeMillis();
            log.info("Creating server socket manager for session {} using port {}", sessionId, port);

            String fileName = getFileName(sessionId);
            File privKeyFile = new File(fileName + ".priv_key");
            PrivateKeyFile pkf = new PrivateKeyFile(privKeyFile);

            try {
                pkf.createIfAbsent();
            } catch (I2PException e) {
                throw new IOException("Could not persist priv key", e);
            }

            I2PSocketManager manager;
            try (FileInputStream privKeyInputStream = new FileInputStream(privKeyFile)) {
                if (!embeddedRouter) {
                    manager = I2PSocketManagerFactory.createManager(privKeyInputStream, host, port, null);
                } else {
                    manager = I2PSocketManagerFactory.createManager(privKeyInputStream);
                    if (manager == null) {
                        manager = i2pRouter.getManager(privKeyFile);
                    }
                }
            }

            I2PSocketOptions options = manager.getDefaultOptions();
            options.setLocalPort(port);
            options.setConnectTimeout(Math.toIntExact(socketTimeout));
            manager.setDefaultOptions(options);

            String destinationBase64 = manager.getSession().getMyDestination().toBase64();
            log.info("My destination: {}", destinationBase64);
            File destinationFile = new File(fileName + ".destination");
            if (!destinationFile.exists()) {
                FileUtils.write(destinationFile.getPath(), destinationBase64);
            }

            sessionMap.put(sessionId, manager);
            log.info("Server socket manager ready for session {}. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
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
}
