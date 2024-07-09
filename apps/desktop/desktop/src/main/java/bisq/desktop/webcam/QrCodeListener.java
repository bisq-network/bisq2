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

@Slf4j
public class QrCodeListener {
    private final int port;
    private final Consumer<String> qrCodeHandler;
    private final Runnable shutdownHandler;
    private volatile boolean isStopped;

    public QrCodeListener(int port, Consumer<String> qrCodeHandler, Runnable shutdownHandler) {
        this.port = port;
        this.qrCodeHandler = qrCodeHandler;
        this.shutdownHandler = shutdownHandler;
    }

    public void start() {
        new Thread(() -> {
            InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", port);
            try (ServerSocket serverSocket = new ServerSocket()) {
                serverSocket.bind(serverAddress);
                log.info("Start listening on port {}", port);
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();
                    try (Scanner scanner = new Scanner(socket.getInputStream())) {
                        StringBuilder stringBuilder = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            stringBuilder.append(line);
                        }

                        String result = stringBuilder.toString();
                        if ("shutdown".equals(result)) {
                            shutdownHandler.run();
                        } else if (!result.isEmpty()) {
                            qrCodeHandler.accept(result);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("Server stopped");
        }).start();
    }

    public void stopServer() {
        log.info("stopServer");
        isStopped = true;
    }
}
