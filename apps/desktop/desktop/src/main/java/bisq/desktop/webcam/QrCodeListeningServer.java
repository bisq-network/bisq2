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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class QrCodeListeningServer {
    private final int port;
    private final Consumer<String> qrCodeHandler;
    private final Runnable shutdownHandler;
    private volatile boolean isStopped;

    public QrCodeListeningServer(int port, Consumer<String> qrCodeHandler, Runnable shutdownHandler) {
        this.port = port;
        this.qrCodeHandler = qrCodeHandler;
        this.shutdownHandler = shutdownHandler;
    }

    public void start() {
        new Thread(() -> {
            InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", port);
            try (ServerSocket serverSocket = new ServerSocket()) {
                // We set 10 seconds timeout. We expect each seond a heartbeat message.
                serverSocket.setSoTimeout(10000);

                serverSocket.bind(serverAddress);

                log.info("Start listening on port {}", port);
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    try (Scanner scanner = new Scanner(socket.getInputStream())) {
                        StringBuilder stringBuilder = new StringBuilder();
                        // We only expect one line
                        while (scanner.hasNextLine() && stringBuilder.length() == 0) {
                            String line = scanner.nextLine();
                            stringBuilder.append(line);
                        }

                        String message = stringBuilder.toString();
                        // LN invoice is usually about 230 chars. We tolerate 1000 in message validation
                        checkArgument(message.length() < 1000, "Received message exceeds out limit of 1000 chars");

                        if (ControlSignals.SHUTDOWN.name().equals(message)) {
                            shutdownHandler.run();
                        } else if (ControlSignals.HEART_BEAT.name().equals(message)) {
                            log.debug(message);
                        } else {
                            log.info("Received: {}", message);
                            qrCodeHandler.accept(message);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Server error", e);
            }
            log.info("Server stopped");
        }).start();
    }

    public void stopServer() {
        log.info("stopServer");
        isStopped = true;
    }
}
