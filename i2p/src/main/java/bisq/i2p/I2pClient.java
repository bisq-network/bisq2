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

package bisq.i2p;

import bisq.common.util.FileUtils;
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
import java.util.concurrent.TimeUnit;

// Streaming API (TCP-like streams over I2P) docs: https://geti2p.net/en/docs/api/streaming
// For UDP-like communication, see datagram spec: https://geti2p.net/spec/datagrams
@Slf4j
public class I2pClient {
    /**
     * "Base32 addresses are much shorter and easier to handle than the full 516-character Base64 Destinations or
     * addresshelpers. Example: ukeu3k5oycgaauneqgtnvselmt4yemvoilkln7jpvamvfx7dnkdq.b32.i2p"
     * <br/><br/>
     *
     * "Base32 lookups will only be successful when the Destination is up and publishing a LeaseSet. Because resolution
     * may require a network database lookup, it may take significantly longer than a local address book lookup."
     * <br/><br/>
     *
     * https://geti2p.net/en/docs/naming#base32
     */
    static final String DESTINATION_EXTENSION_BASE_32 = ".destination.b32.i2p";
    static final String DESTINATION_EXTENSION_BASE_64 = ".destination";

    public final static String DEFAULT_HOST = "127.0.0.1";
    public final static int DEFAULT_PORT = 7656;
    public final static long DEFAULT_SOCKET_TIMEOUT = TimeUnit.MINUTES.toMillis(3);
    private final static Map<String, I2pClient> I2P_CLIENT_BY_APP = new ConcurrentHashMap<>();

    private final String host;
    private final int port;
    private final long socketTimeout;
    private final String dirPath;
    // key = sessionId (relevant in the Bisq domain), value = socket manager at I2P level
    // Each socket manager has one session and one socket (although multiple sockets supported)
    private final Map<String, I2PSocketManager> sessionMap = new ConcurrentHashMap<>();

    public static I2pClient getI2pClient(String dirPath) {
        return getI2pClient(dirPath, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_SOCKET_TIMEOUT);
    }

    // We use one i2p client per app
    public static I2pClient getI2pClient(String dirPath, String host, int port, long socketTimeout) {
        I2pClient i2pClient;
        synchronized (I2P_CLIENT_BY_APP) {
            if (I2P_CLIENT_BY_APP.containsKey(dirPath)) {
                i2pClient = I2P_CLIENT_BY_APP.get(dirPath);
            } else {
                i2pClient = new I2pClient(dirPath, host, port, socketTimeout);
                I2P_CLIENT_BY_APP.put(dirPath, i2pClient);
            }
        }
        return i2pClient;
    }

    private I2pClient(String dirPath, String host, int port, long socketTimeout) {
        this.host = host;
        this.port = port;
        this.socketTimeout = socketTimeout;
        this.dirPath = dirPath;
        log.info("I2P client created with dirPath={}; host={}; port={}; socketTimeout={}",
                dirPath, host, port, socketTimeout);
        configureI2pLogging();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("I2pClient.shutdownHook");
            shutdown();
        }));
    }

    private void configureI2pLogging() {
        /*
          I2P uses a custom log framework.

          There are two ways to change logging-related configs:

          1) Via a config file:
          - The file must be called `logger.config` and must be placed in the current working directory (IdeaProjects/bisq2)
          - For properties available, see net.i2p.util.LogManager.PROP_*

          See https://geti2p.net/spec/configuration -> "Logger (logger.config)"

          Note: A custom config file name and location can be loaded using
          I2PAppContext.getGlobalContext().logManager().setConfig("desktop/src/main/resources/bisq.properties");
          using a path relative to the current working directory.

          2) Using the exposed setters on the log manager:
          I2PAppContext.getGlobalContext().logManager().set*
         */
        String baseLogFilename = dirPath + "/logs/i2p-@.log"; // @ = counter (starts with 0, incremented with every new log file)
        I2PAppContext.getGlobalContext().logManager().setBaseLogfilename(baseLogFilename);
        log.debug("I2P logs to {}", baseLogFilename);
    }

    /**
     * @param peer      Can be *.i2p, *b32.i2p or base 64 addresses. If not base64 we resolve it via name lookup
     * @param sessionId Session ID
     * @return The socket for the outbound connection
     * @throws IOException
     */
    public Socket getSocket(String peer, String sessionId) throws IOException {
        try {
            long ts = System.currentTimeMillis();
            log.debug("Start to create session {}", sessionId);

            Destination destination = I2pUtils.getDestinationFor(peer)
                    .orElseThrow(() -> new IOException("Destination not found for peer " + peer));

            log.info("Connecting to {}", peer);
            // Each client (socket manager) can have multiple sockets
            // However we only open one socket per client => one socket per manager per client
            I2PSocketManager manager = maybeCreateClientSession(sessionId);
            Socket socket = manager.connectToSocket(destination, Math.toIntExact(socketTimeout));
            log.info("Client socket for session {} created. Took {} ms.", sessionId, System.currentTimeMillis() - ts);

            // Now we are done, so we return the socket to be used by the client for sending messages
            return socket;
        } catch (IOException e) {
            handleIOException(e, sessionId);
            throw e;
        }
    }

    public ServerSocket getServerSocket(String sessionId, int port) throws IOException {
        return maybeCreateServerSession(sessionId, port).getStandardServerSocket();
    }

    public String getMyDestination(String sessionId) throws IOException {
        String destinationFileName = getFileName(sessionId) + DESTINATION_EXTENSION_BASE_64;

        // Destination file is stored at the same time when the private key file is written
        return FileUtils.readAsString(destinationFileName);
    }

    public void shutdown() {
        long ts = System.currentTimeMillis();
        sessionMap.values().forEach(I2PSocketManager::destroySocketManager);
        sessionMap.clear();

        // Takes < 20 ms per client
        log.info("I2P shutdown completed. Took {} ms.", System.currentTimeMillis() - ts);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private String getFileName(String sessionId) throws IOException {
        FileUtils.makeDirs(dirPath);
        return dirPath + File.separator + sessionId;
    }

    private I2PSocketManager maybeCreateClientSession(String sessionId) {
        if (!sessionMap.containsKey(sessionId)) {
            sessionMap.put(sessionId, I2PSocketManagerFactory.createManager());
        }

        return sessionMap.get(sessionId);
    }

    private I2PSocketManager maybeCreateServerSession(String sessionId, int port) throws IOException {
        if (!sessionMap.containsKey(sessionId)) {
            long ts = System.currentTimeMillis();
            log.info("Start to create server socket manager for session {} using port {}", sessionId, port);

            String fileName = getFileName(sessionId);
            String privKeyFileName = fileName + ".priv_key";
            File privKeyFile = new File(privKeyFileName);
            PrivateKeyFile pkf = new PrivateKeyFile(privKeyFile);
            try {
                // Persist priv key to disk
                pkf.createIfAbsent();
            } catch (I2PException e) {
                throw new IOException("Could not persist priv key to disk", e);
            }

            // Create a I2PSocketManager based on the locally persisted private key
            // This allows the server to preserve its identity and be reachable at the same destination
            I2PSocketManager manager;
            try(FileInputStream privKeyInputStream = new FileInputStream(privKeyFile)) {
                manager = I2PSocketManagerFactory.createManager(privKeyInputStream);
            }

            // Set port (which is embedded in the generated destination)
            I2PSocketOptions i2PSocketOptions = manager.getDefaultOptions();
            i2PSocketOptions.setLocalPort(port);
            i2PSocketOptions.setConnectTimeout(Math.toIntExact(socketTimeout));
            manager.setDefaultOptions(i2PSocketOptions);

            // Persist destination to disk (base64 format): 516 base-64 chars
            String destinationBase64 = manager.getSession().getMyDestination().toBase64();
            log.debug("My destination (base 64) is {}", destinationBase64);
            String destinationFileNameB64 = fileName + DESTINATION_EXTENSION_BASE_64;
            File destinationFileB64 = new File(destinationFileNameB64);
            if (!destinationFileB64.exists()) {
                FileUtils.write(destinationFileNameB64, destinationBase64);
            }

            // Persist destination to disk (base32 format): "{52 chars}.b32.i2p"
            String destinationBase32 = manager.getSession().getMyDestination().toBase32();
            log.debug("My destination (base 32) is {}", destinationBase32);
            String destinationFileNameB32 = fileName + DESTINATION_EXTENSION_BASE_32;
            File destinationFileB32 = new File(destinationFileNameB32);
            if (!destinationFileB32.exists()) {
                FileUtils.write(destinationFileNameB32, destinationBase32);
            }

            // Takes 10-30 sec
            log.info("Server socket manager ready for session {}. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
            sessionMap.put(sessionId, manager);
        }

        return sessionMap.get(sessionId);
    }

    protected void handleIOException(IOException e, String sessionId) {
        e.printStackTrace();
        I2PSocketManager manager = sessionMap.get(sessionId);
        if (manager != null) {
            manager.destroySocketManager();
        }
        sessionMap.remove(sessionId);
    }
}
