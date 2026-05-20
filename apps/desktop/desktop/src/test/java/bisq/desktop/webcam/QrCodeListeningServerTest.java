/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import bisq.application.ApplicationService;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QrCodeListeningServerTest {
    @TempDir
    Path tempDir;

    @Test
    void setsReadTimeoutOnAcceptedSocket() throws Exception {
        int socketTimeout = 123;
        CompletableFuture<Integer> acceptedSocketTimeout = new CompletableFuture<>();
        QrCodeListeningServer qrCodeListeningServer = new QrCodeListeningServer(socketTimeout,
                inputHandlerCompleting(acceptedSocketTimeout),
                acceptedSocketTimeout::completeExceptionally);
        int port = selectFreePort();

        qrCodeListeningServer.start(port);
        try (Socket ignored = connectToServer(port)) {
            assertEquals(socketTimeout, acceptedSocketTimeout.get(5, TimeUnit.SECONDS));
        } finally {
            qrCodeListeningServer.stopServer();
        }
    }

    private InputHandler inputHandlerCompleting(CompletableFuture<Integer> acceptedSocketTimeout) {
        return new InputHandler(createModel()) {
            @Override
            public void onSocket(Socket socket) {
                try {
                    acceptedSocketTimeout.complete(socket.getSoTimeout());
                } catch (SocketException e) {
                    acceptedSocketTimeout.completeExceptionally(e);
                }
            }
        };
    }

    private WebcamAppModel createModel() {
        return new WebcamAppModel(new ApplicationService.Config(
                ConfigFactory.empty(),
                tempDir,
                "Bisq-Test",
                false,
                0,
                false,
                "",
                false,
                false,
                0,
                false,
                false));
    }

    private static int selectFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
            return serverSocket.getLocalPort();
        }
    }

    private static Socket connectToServer(int port) throws IOException, InterruptedException {
        IOException lastException = null;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try {
                return new Socket("127.0.0.1", port);
            } catch (IOException e) {
                lastException = e;
                Thread.sleep(10);
            }
        }
        throw lastException;
    }
}
