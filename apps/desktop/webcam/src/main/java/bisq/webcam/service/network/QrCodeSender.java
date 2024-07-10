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

package bisq.webcam.service.network;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class QrCodeSender {

    private final InetSocketAddress serverAddress;

    public QrCodeSender(int port) {
        serverAddress = new InetSocketAddress("127.0.0.1", port);
    }

    public CompletableFuture<Void> send(String qrCode) {
        log.info("send {} to {}", qrCode, serverAddress);
        return CompletableFuture.runAsync(() -> {
            try (Socket socket = new Socket();) {
                socket.connect(serverAddress);
                try (PrintWriter printWriter = new PrintWriter(socket.getOutputStream())) {
                    printWriter.println(qrCode);
                }
            } catch (IOException e) {
                log.error("Error at sending qrCode {} to {}", qrCode, serverAddress, e);
                throw new RuntimeException(e);
            }
        }).orTimeout(1, TimeUnit.SECONDS);
    }
}
