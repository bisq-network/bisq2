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

package network.misq.i2p;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

// SAM documentation: https://geti2p.net/en/docs/api/samv3
@Slf4j
public class SamClient {
    public final static String DEFAULT_HOST = "127.0.0.1";
    public final static int DEFAULT_PORT = 7656;
    public final static long DEFAULT_SOCKET_TIMEOUT = TimeUnit.MINUTES.toMillis(3);
    private final static Map<String, SamClient> SAM_CLIENT_BY_APP = new HashMap<>();

    private final String host;
    private final int port;
    private final long socketTimeout;
    private final String dirPath;
    private final Set<String> activeSessions = new HashSet<>();
    private final Set<SamConnection> openSamConnections = new HashSet<>();

    public static SamClient getSamClient(String dirPath) {
        return getSamClient(dirPath, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_SOCKET_TIMEOUT);
    }

    // We use one sam client per app
    public static SamClient getSamClient(String dirPath, String host, int port, long socketTimeout) {
        SamClient samClient;
        synchronized (SAM_CLIENT_BY_APP) {
            if (SAM_CLIENT_BY_APP.containsKey(dirPath)) {
                samClient = SAM_CLIENT_BY_APP.get(dirPath);
            } else {
                samClient = new SamClient(dirPath, host, port, socketTimeout);
                SAM_CLIENT_BY_APP.put(dirPath, samClient);
            }
        }
        return samClient;
    }

    private SamClient(String dirPath, String host, int port, long socketTimeout) {
        this.host = host;
        this.port = port;
        this.socketTimeout = socketTimeout;
        this.dirPath = dirPath;
        log.info("Sam client created with dirPath={}; host={}; port={}; socketTimeout={}",
                dirPath, host, port, socketTimeout);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * @param peer      Can be *.i2p, *b32.i2p or base 64 addresses. If not base64 we resolve it via name lookup
     * @param sessionId Session ID
     * @return The socket for the outbound connection
     * @throws IOException
     */
    public Socket connect(String peer, String sessionId) throws IOException {
        SamConnection samConnection = null;
        try {
            long ts = System.currentTimeMillis();
            log.debug("Start connect handshake for {}", sessionId);

            maybeCreateSession(sessionId);
            samConnection = startSamControlConnection();

            String destination = toBase64Destination(peer, samConnection);

            String request = String.format("STREAM CONNECT ID=%s DESTINATION=%s SILENT=false", sessionId, destination);
            samConnection.doHandShake(request);
            log.info("Connect handshake for {} completed. Took {} ms.", sessionId, System.currentTimeMillis() - ts);

            // Now we are done, so we return the socket to be used by the client for sending messages
            return samConnection.getClientSocket();
        } catch (IOException e) {
            handleIOException(e, sessionId, samConnection);
            throw e;
        }
    }

    public ServerSocket getServerSocket(String sessionId, int port) throws IOException {
        long ts = System.currentTimeMillis();
        log.info("Start forward handshake for {} using port {}", sessionId, port);
        maybeCreateSession(sessionId);
        SamConnection samConnection = startSamControlConnection();

        String request = String.format("STREAM FORWARD ID=%s PORT=%d SILENT=true", sessionId, port);
        samConnection.doHandShake(request);
        log.info("Forward handshake for {} completed. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
        return new ServerSocket(port);
    }

    private SamConnection getConnectionForForward(String sessionId, int port) throws IOException {
        long ts = System.currentTimeMillis();
        log.info("Start forward handshake for {}", sessionId);
        maybeCreateSession(sessionId);
        SamConnection samConnection = startSamControlConnection();

        String request = String.format("STREAM FORWARD ID=%s PORT=%d SILENT=true", sessionId, port);
        samConnection.doHandShake(request);
        log.info("Forward handshake for {} completed. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
        return samConnection;
    }

    public I2pServerSocket getServerSocket(String sessionId) throws IOException {
        try {
            Supplier<Socket> socketSupplier = () -> {
                try {
                    long ts = System.currentTimeMillis();
                    SamConnection samConnection = getConnectionForAccept(sessionId);
                    log.error("Establishing server socket took {} ms. Waiting for incoming connections now.",
                            System.currentTimeMillis() - ts);

                    // Blocking wait for inbound connection
                    return samConnection.listenForClient();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            };

            return new I2pServerSocket(port, socketSupplier);
        } catch (IOException e) {
            handleIOException(e, sessionId, null);
            throw e;
        }
    }

    public String getMyDestination(String sessionId) throws IOException {
        FileUtils.makeDirIfNotExists(dirPath);
        String fileName = dirPath + File.separator + sessionId;
        String destinationFileName = fileName + ".destination";
        if (new File(destinationFileName).exists()) {
            return FileUtils.readAsString(destinationFileName);
        } else {
            try {
                maybeCreateSession(sessionId);
                SamConnection samConnection = startSamControlConnection();
                Reply reply = samConnection.doHandShake("DEST GENERATE SIGNATURE_TYPE=7");
                String destination = reply.get("PUB");
                FileUtils.write(destinationFileName, destination);

                // We also store the private key
                String privKeyFileName = fileName + ".priv_key";
                String privateKeyBase64 = reply.get("PRIV");
                FileUtils.write(privKeyFileName, privateKeyBase64);
                return destination;
            } catch (IOException e) {
                handleIOException(e, sessionId, null);
                throw e;
            }
        }
    }

    public void shutdown() {
        openSamConnections.forEach(SamConnection::close);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private SamConnection getConnectionForAccept(String sessionId) throws IOException {
        maybeCreateSession(sessionId);
        SamConnection samConnection = startSamControlConnection();

        String request = String.format("STREAM ACCEPT ID=%s SILENT=true", sessionId);
        samConnection.doHandShake(request);
        return samConnection;
    }

    private SamConnection startSamControlConnection() throws IOException {
        SamConnection samConnection = new SamConnection(host, port, socketTimeout);
        openSamConnections.add(samConnection);
        String request = "HELLO VERSION MIN=3.1 MAX=3.1";
        samConnection.doHandShake(request);
        return samConnection;
    }

    private void maybeCreateSession(String sessionId) throws IOException {
        if (activeSessions.contains(sessionId)) {
            return;
        }
        long ts = System.currentTimeMillis();
        log.info("Start creating session for {}", sessionId);
        SamConnection samConnection;
        try {
            samConnection = startSamControlConnection();
        } catch (ConnectException connectException) {
            log.warn("Could not connect to I2P. This might be expected at startup if I2P is not ready yet. " +
                    "We will try again after a delay. connectException={}", connectException.toString());
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException ignore) {
            }
            maybeCreateSession(sessionId);
            return;
        }
        checkNotNull(samConnection);
        String privateKeyBase64 = loadOrRequestPrivateKey(samConnection, sessionId);

        String request = String.format("SESSION CREATE STYLE=STREAM ID=%s DESTINATION=%s", sessionId, privateKeyBase64);
        samConnection.doHandShake(request);
        activeSessions.add(sessionId);
        log.info("Session for {} created. Took {} ms.", sessionId, System.currentTimeMillis() - ts);
    }

    private String toBase64Destination(String destination, SamConnection samConnection) throws IOException {
        if (isBase64Destination(destination)) {
            return destination;
        }

        String request = "NAMING LOOKUP NAME=" + destination;
        Reply lookupReply = samConnection.doHandShake(request);
        return lookupReply.get("VALUE");
    }

    private String loadOrRequestPrivateKey(SamConnection samConnection,
                                           String sessionId) throws IOException {
        FileUtils.makeDirIfNotExists(dirPath);
        String fileName = dirPath + File.separator + sessionId;
        String privKeyFileName = fileName + ".priv_key";
        if (new File(privKeyFileName).exists()) {
            return FileUtils.readAsString(privKeyFileName);
        }

        // DEST GENERATE does not require that a session has been created first.
        Reply reply = samConnection.doHandShake("DEST GENERATE SIGNATURE_TYPE=7");
        FileUtils.write(fileName + ".destination", reply.get("PUB"));
        String privateKeyBase64 = reply.get("PRIV");
        FileUtils.write(privKeyFileName, privateKeyBase64);
        return privateKeyBase64;
    }

    private boolean isBase64Destination(String peer) {
        return peer.endsWith("AAA==");
    }

    protected void handleIOException(IOException e, String sessionId, SamConnection samConnection) {
        activeSessions.remove(sessionId);
        if (samConnection != null) {
            samConnection.close();
            openSamConnections.remove(samConnection);
        }
    }
}
