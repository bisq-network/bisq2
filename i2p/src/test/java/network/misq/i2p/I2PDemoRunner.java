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
import network.misq.common.util.NetworkUtils;
import network.misq.common.util.OsUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

// Requires local i2p installation.
// Start I2P.
// Enable SAM at http://127.0.0.1:7657/configclients
// Takes about 1 minute until its ready
@Slf4j
public class I2PDemoRunner {
    private final SamClient samClient;

    public static void main(String[] args) throws IOException {
        new I2PDemoRunner();
    }

    public I2PDemoRunner() throws IOException {
        String dirPath = OsUtils.getUserDataDir() + "/I2PDemoRunner";
        FileUtils.makeDirs(dirPath);
        samClient = SamClient.getSamClient(dirPath);
        String sessionIdAlice = "alice";
        String sessionIdBob = "bob";
        String sessionIdCarol = "carol";

        AtomicReference<String> destinationAlice = new AtomicReference<>();
        AtomicReference<Boolean> aliceServerReady = new AtomicReference<>(false);
        new Thread(() -> {
            Thread.currentThread().setName("Alice");
            try {
                destinationAlice.set(samClient.getMyDestination(sessionIdAlice));
                ServerSocket serverSocket = samClient.getServerSocket(sessionIdAlice, NetworkUtils.findFreeSystemPort());
                startServer(serverSocket, aliceServerReady);
            } catch (IOException e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
            }
        }).start();

        AtomicReference<String> destinationBob = new AtomicReference<>();
        new Thread(() -> {
            Thread.currentThread().setName("Bob");
            try {
                destinationBob.set(samClient.getMyDestination(sessionIdBob));
                while (!aliceServerReady.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }

                Socket clientSocketBobToAlice = samClient.connect(destinationAlice.get(), sessionIdBob);
                PrintWriter printWriter = new PrintWriter(clientSocketBobToAlice.getOutputStream(), true);
                printWriter.println("####### Hello from Bob1");
                printWriter.flush();
                log.info("Bob sent message1");

                Thread.sleep(100);
                printWriter.println("####### Hello from Bob2");
                printWriter.flush();
                log.info("Bob sent message2");
            } catch (IOException | InterruptedException e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
            }
        }).start();

        AtomicReference<String> destinationCarol = new AtomicReference<>();
        new Thread(() -> {
            Thread.currentThread().setName("Carol");
            try {
                destinationCarol.set(samClient.getMyDestination(sessionIdCarol));
                while (!aliceServerReady.get()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }
                Socket clientSocketCarolToAlice = samClient.connect(destinationAlice.get(), sessionIdCarol);
                PrintWriter printWriter = new PrintWriter(clientSocketCarolToAlice.getOutputStream(), true);
                printWriter.println("####### Hello from Carol1");
                printWriter.flush();
                log.info("Carol sent message1");

                Thread.sleep(100);
                printWriter.println("####### Hello from Carol2");
                printWriter.flush();
                log.info("Carol sent message2");


                Thread.sleep(1000);
                printWriter.println("stop");
                printWriter.flush();
                log.info("Carol sent stop");
            } catch (IOException | InterruptedException e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startServer(ServerSocket serverSocket, AtomicReference<Boolean> aliceServerReady) {
        new Thread(() -> {
            Thread.currentThread().setName("Server");
            int c = 0;
            while (c < 10 && !Thread.currentThread().isInterrupted()) {
                c++;
                try {
                    aliceServerReady.set(true);
                    log.error("Waiting for inbound connection");
                    Socket clientSocket = serverSocket.accept();
                    // aliceServerReady.set(false);
                    onNewConnection(clientSocket);
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void onNewConnection(Socket clientSocket) {
        new Thread(() -> {
            Thread.currentThread().setName("Client");
            while (!clientSocket.isClosed()) {
                try (InputStream inputStream = clientSocket.getInputStream();
                     Scanner scanner = new Scanner(inputStream)) {
                    while (scanner.hasNextLine()) {
                        String msg = scanner.nextLine();
                        log.info("""
                                        Inbound message
                                        Peer: {}
                                        Message: {}""",
                                clientSocket.getRemoteSocketAddress(),
                                msg);
                        if (msg.equals("stop")) {
                            samClient.shutdown();
                        }
                    }
                } catch (IOException e) {
                    if (!(e instanceof SocketException)) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void startServer(I2pServerSocket serverSocket) {
        new Thread(() -> {
            Thread.currentThread().setName("Server");
            int c = 0;
            while (c < 10 && !Thread.currentThread().isInterrupted()) {
                c++;
                Socket clientSocket = serverSocket.accept();
                onNewConnection(clientSocket);

            }
        }).start();
    }
}
