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

package network.misq;

import com.runjva.sourceforge.jsocks.protocol.SocksSocket;
import network.misq.common.util.OsUtils;
import network.misq.tor.OnionAddress;
import network.misq.tor.Tor;
import network.misq.tor.TorServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TorDemo {
    private static final Logger log = LoggerFactory.getLogger(TorDemo.class);
    private static Tor tor;

    public static void main(String[] args) throws InterruptedException {
        String torDirPath = OsUtils.getUserDataDir() + "/TorDemo";
        //   useBlockingAPI(torDirPath);
        useNonBlockingAPI(torDirPath);
    }

    private static void useBlockingAPI(String torDirPath) {
        try {
            tor = Tor.getTor(torDirPath);
            tor.start();
            TorServerSocket torServerSocket = startServer();
            OnionAddress onionAddress = torServerSocket.getOnionAddress().get();
            sendViaSocketFactory(tor, onionAddress);
            sendViaProxy(tor, onionAddress);
            sendViaSocket(tor, onionAddress);
            sendViaSocksSocket(tor, onionAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void useNonBlockingAPI(String torDirPath) throws InterruptedException {
        AtomicBoolean stopped = new AtomicBoolean(false);
        tor = Tor.getTor(torDirPath);
        CountDownLatch latch = new CountDownLatch(1);
        tor.startAsync()
                .thenCompose(result -> startServerAsync()
                        .thenAccept(onionAddress -> {
                            if (onionAddress == null) {
                                return;
                            }

                            sendViaSocketFactory(tor, onionAddress);
                            sendViaProxy(tor, onionAddress);
                            sendViaSocket(tor, onionAddress);
                            sendViaSocksSocket(tor, onionAddress);
                            latch.countDown();
                        }));

        latch.await(2, TimeUnit.MINUTES);
    }

    private static TorServerSocket startServer() throws IOException, InterruptedException {
        TorServerSocket torServerSocket = tor.getTorServerSocket();
        torServerSocket.bind(4000, 9999, "hiddenservice_2");
        runServer(torServerSocket);
        return torServerSocket;
    }

    private static CompletableFuture<OnionAddress> startServerAsync() {
        CompletableFuture<OnionAddress> future = new CompletableFuture<>();
        try {
            TorServerSocket torServerSocket = tor.getTorServerSocket();
            torServerSocket
                    .bindAsync(3000, "hiddenservice_3")
                    .whenComplete((onionAddress, throwable) -> {
                        if (throwable == null) {
                            runServer(torServerSocket);
                            future.complete(onionAddress);
                        } else {
                            future.completeExceptionally(throwable);
                        }
                    });
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private static void runServer(TorServerSocket torServerSocket) {
        new Thread(() -> {
            Thread.currentThread().setName("Server");
            while (true) {
                try {
                    log.info("Start listening for new connections on {}", torServerSocket.getOnionAddress());
                    Socket clientSocket = torServerSocket.accept();
                    createInboundConnection(clientSocket);
                } catch (IOException e) {
                    try {
                        torServerSocket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }).start();
    }

    private static void createInboundConnection(Socket clientSocket) {
        log.info("New client connection accepted");
        new Thread(() -> {
            Thread.currentThread().setName("Read at inbound connection");
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream())) {
                objectOutputStream.flush();
                listenOnInputStream(clientSocket, objectInputStream, "inbound connection");
            } catch (IOException e) {
                try {
                    clientSocket.close();
                } catch (IOException ignore) {
                }
            }
        }).start();
    }

    private static void listenOnInputStream(Socket socket, ObjectInputStream objectInputStream, String info) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Object object = objectInputStream.readObject();
                log.info("Received at {} {}", info, object);
            }
        } catch (IOException | ClassNotFoundException e) {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }

    // Outbound connection
    private static void sendViaSocket(Tor tor, OnionAddress onionAddress) {
        try {
            Socket socket = tor.getSocket("test_stream_id");
            socket.connect(new InetSocketAddress(onionAddress.getHost(), onionAddress.getPort()));
            sendOnOutboundConnection(socket, "test via Socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendViaSocksSocket(Tor tor, OnionAddress onionAddress) {
        try {
            SocksSocket socket = tor.getSocksSocket(onionAddress.getHost(), onionAddress.getPort(), "test_stream_id");
            sendOnOutboundConnection(socket, "test via SocksSocket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendViaSocketFactory(Tor tor, OnionAddress onionAddress) {
        try {
            SocketFactory socketFactory = tor.getSocketFactory("test_stream_id");
            Socket socket = socketFactory.createSocket(onionAddress.getHost(), onionAddress.getPort());
            sendOnOutboundConnection(socket, "test via SocketFactory");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendViaProxy(Tor tor, OnionAddress onionAddress) {
        try {
            Proxy proxy = tor.getProxy("test_stream_id");
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(onionAddress.getHost(), onionAddress.getPort()));
            sendOnOutboundConnection(socket, "test via Proxy");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendOnOutboundConnection(Socket socket, String msg) {
        log.info("sendViaOutboundConnection {}", msg);
        new Thread(() -> {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())) {
                objectOutputStream.writeObject(msg);
                objectOutputStream.flush();
                listenOnInputStream(socket, objectInputStream, "outbound connection");
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }).start();
    }
}
