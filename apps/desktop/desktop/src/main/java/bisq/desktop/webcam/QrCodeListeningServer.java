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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
public class QrCodeListeningServer {
    private final int socketTimeout;
    private final InputHandler inputHandler;
    private final Consumer<Exception> errorHandler;
    private volatile boolean isStopped;
    private Optional<ServerSocket> serverSocket = Optional.empty();

    public QrCodeListeningServer(int socketTimeout, InputHandler inputHandler, Consumer<Exception> errorHandler) {
        this.socketTimeout = socketTimeout;
        this.inputHandler = inputHandler;
        this.errorHandler = errorHandler;
    }

    public void start(int port) {
        isStopped = false;
        serverSocket = Optional.empty();

        CompletableFuture.runAsync(() -> {
            InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", port);
            try (ServerSocket serverSocket = new ServerSocket()) {
                this.serverSocket = Optional.of(serverSocket);
                serverSocket.setSoTimeout(socketTimeout);
                serverSocket.bind(serverAddress);
                log.info("Start listening on port {}", port);
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    inputHandler.onSocket(socket);
                }
            } catch (IOException e) {
                if (!isStopped) {
                    log.error("Server error", e);
                    errorHandler.accept(e);
                }
            }
            log.info("Server stopped");
        }, ExecutorFactory.newSingleThreadExecutor("QrCodeListeningServer"));
    }

    public void stopServer() {
        log.info("stopServer");
        if (isStopped) {
            return;
        }

        isStopped = true;
        serverSocket.ifPresent(serverSocket -> {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        });
    }
}
