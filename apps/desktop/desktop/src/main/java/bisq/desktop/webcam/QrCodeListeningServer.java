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

package bisq.desktop.webcam;

import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class QrCodeListeningServer {
    private static final String LOOPBACK_HOST = "127.0.0.1";

    private final int socketTimeout;
    private final InputHandler inputHandler;
    private final Consumer<Exception> errorHandler;
    private volatile boolean isStopped;
    private volatile Optional<ServerSocket> serverSocket = Optional.empty();

    public QrCodeListeningServer(int socketTimeout, InputHandler inputHandler, Consumer<Exception> errorHandler) {
        this.socketTimeout = socketTimeout;
        this.inputHandler = inputHandler;
        this.errorHandler = errorHandler;
    }

    public int start() {
        return start(0);
    }

    public int start(int port) {
        isStopped = false;
        serverSocket = Optional.empty();

        InetSocketAddress serverAddress = new InetSocketAddress(LOOPBACK_HOST, port);
        try {
            ServerSocket boundServerSocket = new ServerSocket();
            boundServerSocket.setSoTimeout(socketTimeout);
            boundServerSocket.bind(serverAddress);
            serverSocket = Optional.of(boundServerSocket);
            int boundPort = boundServerSocket.getLocalPort();

            try {
                CompletableFuture.runAsync(() -> listen(boundServerSocket),
                        ExecutorFactory.newSingleThreadExecutor("QrCodeListeningServer"));
            } catch (RuntimeException e) {
                close(boundServerSocket);
                serverSocket = Optional.empty();
                throw e;
            }

            return boundPort;
        } catch (IOException e) {
            log.error("Server start failed", e);
            throw new IllegalStateException("Could not start webcam IPC server", e);
        }
    }

    private void listen(ServerSocket boundServerSocket) {
        try (boundServerSocket) {
            log.info("Start listening for webcam IPC");
            while (!isStopped && !Thread.currentThread().isInterrupted()) {
                try (Socket socket = boundServerSocket.accept()) {
                    socket.setSoTimeout(socketTimeout);
                    inputHandler.onSocket(socket);
                } catch (SocketTimeoutException ignore) {
                } catch (IllegalArgumentException e) {
                    log.warn("Rejected webcam IPC message: {}", e.getMessage());
                } catch (RuntimeException e) {
                    log.error("Unexpected webcam IPC handler failure", e);
                    errorHandler.accept(e);
                    throw e;
                }
            }
        } catch (IOException e) {
            if (!isStopped) {
                log.error("Server error", e);
                errorHandler.accept(e);
            }
        } finally {
            clearServerSocket(boundServerSocket);
            log.info("Server stopped");
        }
    }

    private void clearServerSocket(ServerSocket boundServerSocket) {
        Optional<ServerSocket> currentServerSocket = serverSocket;
        if (currentServerSocket.isPresent() && currentServerSocket.get() == boundServerSocket) {
            serverSocket = Optional.empty();
        }
    }

    private void close(ServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
    }

    public void stopServer() {
        if (isStopped) {
            return;
        }

        isStopped = true;
        serverSocket.ifPresent(serverSocket -> {
            log.info("stopServer");
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        });
    }
}
