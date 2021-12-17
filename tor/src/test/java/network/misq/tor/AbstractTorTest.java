package network.misq.tor;


import com.runjva.sourceforge.jsocks.protocol.SocksSocket;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.FileUtils;
import network.misq.common.util.OsUtils;

import javax.net.SocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.String.format;
import static network.misq.tor.Constants.VERSION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
abstract class AbstractTorTest {

    private static final String TEST_STREAM_ID = "test_stream_id";

    private enum ConnectionType {
        INBOUND,
        OUTBOUND
    }

    protected static Tor tor;
    protected static TorServerSocket torServerSocket;
    protected static OnionAddress onionAddress;
    protected static boolean isShutdown = false;
    protected static String expectedMessage;
    protected static long sendTs;
    protected static long startConnectionTs;

    protected final Supplier<String> torTestDirPathSpec = () ->
            OsUtils.getUserDataDir() + File.separator + this.getClass().getSimpleName();
    protected final Predicate<Exception> isClassNotFoundException = (ex) -> ex instanceof ClassNotFoundException;
    protected final Predicate<Exception> isEOFException = (ex) -> ex instanceof EOFException;
    protected final Predicate<Exception> isSocketClosedException = (ex) ->
            ex instanceof SocketException && ex.getMessage().equals("Socket closed");

    public static void cleanTorInstallDir(String torDirPathSpec) {
        File torDir = new File(torDirPathSpec);
        if (torDir.exists()) {
            log.info("Cleaning tor install dir {}", torDirPathSpec);
            FileUtils.deleteDirectory(torDir);
        }
        File versionFile = new File(torDir, VERSION);
        assertFalse(versionFile.exists());
    }

    protected TorServerSocket startServer() throws IOException, InterruptedException {
        TorServerSocket torServerSocket = tor.getTorServerSocket();
        torServerSocket.bind(4000, 9999, "hiddenservice_2");
        runServer(torServerSocket);
        return torServerSocket;
    }

    protected CompletableFuture<OnionAddress> startServerAsync() {
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

    protected void runServer(TorServerSocket torServerSocket) {
        new Thread(() -> {
            Thread.currentThread().setName("Server");
            while (true) {
                try {
                    log.info("Start listening for connections on {}.", torServerSocket.getOnionAddress());
                    Socket clientSocket = torServerSocket.accept();
                    createInboundConnection(clientSocket);
                } catch (IOException ex) {
                    try {
                        if (isShutdown && isSocketClosedException.test(ex)) {
                            log.info("Socket already closed prior to Tor shutdown.");
                            return;
                        } else {
                            torServerSocket.close();
                            fail(ex);
                        }
                    } catch (IOException ignore) {
                        // empty
                    }
                }
            }
        }).start();
    }

    protected void createInboundConnection(Socket clientSocket) {
        log.info("New client connection accepted");
        new Thread(() -> {
            Thread.currentThread().setName("Read at inbound connection");
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream())) {
                objectOutputStream.flush();

                // listenOnInputStream(clientSocket, objectInputStream, "inbound connection");
                listenOnInputStream(clientSocket, objectInputStream, ConnectionType.INBOUND);

            } catch (IOException ex) {
                try {
                    clientSocket.close();
                    if (!isEOFException.test(ex))
                        fail(ex);
                } catch (IOException ignore) {
                    // empty
                }
            }
        }).start();
    }

    protected void listenOnInputStream(Socket socket,
                                       ObjectInputStream objectInputStream,
                                       ConnectionType connectionType) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Object message = objectInputStream.readObject();
                log.info("Received '{}' on {} connection.",
                        message,
                        connectionType.name().toLowerCase());
                if (connectionType.equals(ConnectionType.INBOUND)) {
                    log.info("Received message after {} ms", System.currentTimeMillis() - sendTs);
                    if (!message.equals(expectedMessage)) {
                        fail(format("Did not read expected message from input stream.  Was '%s', expected '%s'",
                                expectedMessage,
                                message));
                    }
                }
            }
        } catch (ClassNotFoundException | IOException ex) {
            closeClientSocketOnException(socket, ex);
        }
    }

    private void closeClientSocketOnException(Socket socket, Exception exception) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // empty
        }
        if (!isEOFException.test(exception) || isClassNotFoundException.test(exception))
            fail(exception);
    }

    // Outbound Connection

    protected void sendViaSocket(Tor tor, OnionAddress onionAddress) {
        try {
            startConnectionTs = System.currentTimeMillis();
            Socket socket = tor.getSocket(TEST_STREAM_ID);
            socket.connect(new InetSocketAddress(onionAddress.getHost(), onionAddress.getPort()));
            sendOnOutboundConnection(socket, "Message via Socket");
        } catch (IOException ex) {
            fail(ex);
        }
    }

    protected void sendViaSocksSocket(Tor tor, OnionAddress onionAddress) {
        try {
            startConnectionTs = System.currentTimeMillis();
            SocksSocket socket = tor.getSocksSocket(onionAddress.getHost(), onionAddress.getPort(), TEST_STREAM_ID);
            sendOnOutboundConnection(socket, "Message via SocksSocket");
        } catch (IOException ex) {
            fail(ex);
        }
    }

    protected void sendViaSocketFactory(Tor tor, OnionAddress onionAddress) {
        try {
            startConnectionTs = System.currentTimeMillis();
            SocketFactory socketFactory = tor.getSocketFactory(TEST_STREAM_ID);
            Socket socket = socketFactory.createSocket(onionAddress.getHost(), onionAddress.getPort());
            sendOnOutboundConnection(socket, "Message via SocketFactory");
        } catch (IOException ex) {
            fail(ex);
        }
    }

    protected void sendViaProxy(Tor tor, OnionAddress onionAddress) {
        try {
            startConnectionTs = System.currentTimeMillis();
            Proxy proxy = tor.getProxy(TEST_STREAM_ID);
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(onionAddress.getHost(), onionAddress.getPort()));
            sendOnOutboundConnection(socket, "Message via Proxy");
        } catch (IOException ex) {
            fail(ex);
        }
    }

    protected void sendOnOutboundConnection(Socket socket, String message) {
        log.info("Connection established after {} ms", System.currentTimeMillis() - startConnectionTs);
        log.info("sendOnOutboundConnection: '{}'", message);
        new Thread(() -> {
            sendTs = System.currentTimeMillis();
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())) {
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();

                expectedMessage = message;
                listenOnInputStream(socket, objectInputStream, ConnectionType.OUTBOUND);

            } catch (IOException ex) {
                try {
                    socket.close();
                    if (!isEOFException.test(ex))
                        fail(ex);
                } catch (IOException ignore) {
                    // empty
                }
            }
        }).start();
    }
}
