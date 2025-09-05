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

package bisq.network.i2p.router;

import bisq.common.observable.Pin;
import bisq.common.threading.ExecutorFactory;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterMonitor;
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Destination;
import net.i2p.router.Router;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
class I2PHttpProxy {
    private final RouterMonitor routerMonitor;
    private final String host;
    private final int port;
    private I2PSocketManager socketManager;
    public ExecutorService serverListenExecutor;
    public ExecutorService handleClientExecutor;
    private final AtomicBoolean isStopped = new AtomicBoolean(false);
    private Pin processStatePin;
    private volatile ServerSocket serverSocket;

    I2PHttpProxy(Router router, RouterMonitor routerMonitor, String host, int port) {
        this.routerMonitor = routerMonitor;
        this.host = host;
        this.port = port;
    }

    void initialize() {
        processStatePin = routerMonitor.getProcessState().addObserver(processState -> {
            if (processState == ProcessState.RUNNING) {
                processStatePin.unbind();
                processStatePin = null;
                if (socketManager != null) {
                    return;
                }
                try {
                    socketManager = I2PSocketManagerFactory.createManager();
                    checkNotNull(socketManager, "socketManager must not be null after processState is RUNNING");
                    startServer();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    void shutdown() {
        if (isStopped.get()) {
            return;
        }
        isStopped.set(true);
        if (processStatePin != null) {
            processStatePin.unbind();
            processStatePin = null;
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close(); // unblocks accept()
            } catch (IOException ignore) {
            }
        }
        ExecutorFactory.shutdownAndAwaitTermination(serverListenExecutor);
        ExecutorFactory.shutdownAndAwaitTermination(handleClientExecutor);
    }

    private void startServer() {
        handleClientExecutor = ExecutorFactory.newCachedThreadPool("I2PHttpProxyServer.handleClient", 1, 4, 30);
        serverListenExecutor = ExecutorFactory.newSingleThreadExecutor("I2PHttpProxyServer.listen");
        serverListenExecutor.submit(() -> {
            try {
                serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
                log.info("I2P HTTP proxy server listening on {}:{}", host, port);
                while (isNotStopped()) {
                    Socket client = serverSocket.accept();
                    handleClientExecutor.submit(() -> handleClient(client));
                }
            } catch (IOException e) {
                if (!isStopped.get()) {
                    log.error("Failed to start server", e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void handleClient(Socket clientSocket) {
        log.info("New client accepted: {}", clientSocket);
        if (isStopped.get()) {
            return;
        }
        Thread.currentThread().setName("I2PHttpProxyServer.handleClient");
        I2PSocket i2pSocket = null;
        try (clientSocket;
             InputStream localIn = clientSocket.getInputStream();
             OutputStream localOut = clientSocket.getOutputStream()) {

            byte[] headerBytes = readHttpHeaders(localIn);
            String headerStr = new String(headerBytes, StandardCharsets.ISO_8859_1);
            String hostHeader = null;

            for (String hLine : headerStr.split("\\r?\\n")) {
                if (hLine.regionMatches(true, 0, "Host:", 0, 5)) {
                    hostHeader = hLine.substring(5).trim();
                    break;
                }
            }

            String lookupHost = null;
            int port = 80; // default HTTP port
            if (hostHeader != null) {
                int colon = hostHeader.indexOf(':');
                if (colon >= 0) {
                    port = Integer.parseInt(hostHeader.substring(colon + 1));
                    log.warn("Port {} detected but will be ignored for I2P site because most eepsites are HTTP/80 internally.", port);
                    lookupHost = hostHeader.substring(0, colon);
                } else {
                    lookupHost = hostHeader;
                }
            }

            if (lookupHost == null || !lookupHost.endsWith(".i2p")) {
                localOut.write("HTTP/1.1 502 Bad Gateway\r\n\r\nOnly .i2p hosts are supported".getBytes());
                return;
            }

            // Resolve I2P destination
            Destination destination = socketManager.getSession().lookupDest(lookupHost);
            if (destination == null) {
                localOut.write("HTTP/1.1 404 Not Found\r\n\r\nI2P destination not found".getBytes());
                return;
            }

            i2pSocket = socketManager.connect(destination);

            OutputStream remoteOut = i2pSocket.getOutputStream();
            InputStream remoteIn = i2pSocket.getInputStream();

            // Send headers to remote
            remoteOut.write(headerBytes);
            remoteOut.flush();

            // Client->Remote (body and any further data)
            Thread up = new Thread(() -> pipe(localIn, remoteOut), "I2PHttpProxyServer.pipe.up");
            up.setDaemon(true);
            up.start();

            // Remote->Client
            pipe(remoteIn, localOut);
        } catch (Exception e) {
            log.error("handleClient failed ", e);
            try {
                clientSocket.getOutputStream().write(("HTTP/1.1 500 Internal Server Error\r\n\r\n" +
                        e.getMessage()).getBytes());
            } catch (IOException ignored) {
            }
        } finally {
            if (i2pSocket != null) {
                try {
                    i2pSocket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static byte[] readHttpHeaders(InputStream in) throws IOException {
        final int MAX = 64 * 1024;
        int state = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(8192);
        while (out.size() <= MAX) {
            int b = in.read();
            if (b == -1) break;
            out.write(b);
            switch (state) {
                case 0 -> state = (b == '\r') ? 1 : 0;
                case 1 -> state = (b == '\n') ? 2 : 0;
                case 2 -> state = (b == '\r') ? 3 : 0;
                case 3 -> {
                    if (b == '\n') return out.toByteArray();
                    state = 0;
                }
            }
        }
        throw new IOException("HTTP header too large or malformed");
    }

    private static void pipe(InputStream in, OutputStream out) {
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                out.flush();
            }
        } catch (IOException e) {
            log.warn("Failed to write to output stream", e);
        } finally {
            try {
                out.flush();
            } catch (IOException ignore) {
            }
        }
    }

    private boolean isNotStopped() {
        return !isStopped.get() && !Thread.currentThread().isInterrupted();
    }
}
