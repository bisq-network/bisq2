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

package bisq.network.http;

import bisq.network.http.utils.Socks5ProxyProvider;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorHttpClientTest {
    private static final String HOST = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvwx.onion";
    private static final int TIMEOUT = 10_000;

    private final ServerSocket serverSocket = new ServerSocket(0, 2, InetAddress.getLoopbackAddress());

    // What the fake proxy received on one connection
    private record Connection(String username, String password, String host, int port, String requestLine) {
    }

    TorHttpClientTest() throws IOException {
    }

    @AfterEach
    void tearDown() throws IOException {
        serverSocket.close();
    }

    @Test
    void eachRequestUsesItsOwnSocksCredentials() throws Exception {
        CompletableFuture<List<Connection>> connections = serve(2);
        TorHttpClient client = createClient();

        assertEquals("ok", client.get("first", Optional.empty()));
        assertEquals("ok", client.get("second", Optional.empty()));

        List<Connection> received = connections.get(TIMEOUT, TimeUnit.MILLISECONDS);
        Connection first = received.get(0);
        Connection second = received.get(1);
        assertEquals(HOST, first.host());
        assertEquals(80, first.port());
        assertEquals("GET /first HTTP/1.1", first.requestLine());
        assertEquals("GET /second HTTP/1.1", second.requestLine());
        assertEquals("0", first.password());
        assertNotEquals(first.username(), second.username());
    }

    // Tor answers the SOCKS5 connect request only after it has built the circuit and reached the server, which can
    // take seconds. shutdown() must abort the request while it waits, instead of waiting for the connection manager,
    // which holds its lock for the whole connect.
    @Test
    void shutdownAbortsRequestWhileSocksConnectIsPending() throws Exception {
        CountDownLatch connectRequestReceived = new CountDownLatch(1);
        CompletableFuture<Void> proxy = serveWithoutAnsweringConnectRequest(connectRequestReceived);
        TorHttpClient client = createClient();

        CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> {
            try {
                return client.get("pending", Optional.empty());
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
        assertTrue(connectRequestReceived.await(TIMEOUT, TimeUnit.MILLISECONDS));

        assertTrue(client.shutdown().get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertThrows(ExecutionException.class, () -> request.get(TIMEOUT, TimeUnit.MILLISECONDS));
        // Completes when the client has closed the connection of the aborted request
        proxy.get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private TorHttpClient createClient() throws IOException {
        Socks5ProxyProvider proxyProvider = new Socks5ProxyProvider(new Socks5Proxy("127.0.0.1", serverSocket.getLocalPort()));
        return new TorHttpClient("http://" + HOST, "http://" + HOST, "test", proxyProvider);
    }

    // A SOCKS5 proxy which accepts the given number of connections one after the other. On each connection it performs
    // the SOCKS5 handshake, reads the HTTP request, and answers "ok". The future completes with what it received.
    private CompletableFuture<List<Connection>> serve(int numConnections) {
        CompletableFuture<List<Connection>> result = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            List<Connection> connections = new ArrayList<>();
            try {
                for (int i = 0; i < numConnections; i++) {
                    try (Socket socket = serverSocket.accept()) {
                        socket.setSoTimeout(TIMEOUT);
                        connections.add(handle(socket));
                    }
                }
                result.complete(connections);
            } catch (IOException | RuntimeException | AssertionError e) {
                result.completeExceptionally(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        return result;
    }

    // A SOCKS5 proxy which performs the authentication of one connection and then leaves the connect request
    // unanswered, like Tor while it builds the circuit. It keeps the connection open until the client closes it.
    // The future completes when the client has closed it, and fails if the proxy got something unexpected.
    private CompletableFuture<Void> serveWithoutAnsweringConnectRequest(CountDownLatch connectRequestReceived) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(TIMEOUT);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                OutputStream out = socket.getOutputStream();

                assertEquals(5, in.readByte());
                in.readFully(new byte[in.readUnsignedByte()]);
                out.write(new byte[]{5, 2});

                assertEquals(1, in.readByte());
                readString(in);
                readString(in);
                out.write(new byte[]{1, 0});

                // The first byte of the connect request, which stays unanswered
                assertEquals(5, in.readByte());
                connectRequestReceived.countDown();
                try {
                    in.readAllBytes();
                } catch (SocketException ignore) {
                    // The client may close the connection with a reset
                }
                result.complete(null);
            } catch (IOException | RuntimeException | AssertionError e) {
                result.completeExceptionally(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        return result;
    }

    private static Connection handle(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        OutputStream out = socket.getOutputStream();

        // The client must offer only username and password authentication
        assertEquals(5, in.readByte());
        byte[] methods = new byte[in.readUnsignedByte()];
        in.readFully(methods);
        assertArrayEquals(new byte[]{2}, methods);
        out.write(new byte[]{5, 2});

        assertEquals(1, in.readByte());
        String username = readString(in);
        String password = readString(in);
        out.write(new byte[]{1, 0});

        // Connect request with a domain name
        assertArrayEquals(new byte[]{5, 1, 0, 3}, in.readNBytes(4));
        String host = readString(in);
        int port = in.readUnsignedShort();
        out.write(new byte[]{5, 0, 0, 1, 0, 0, 0, 0, 0, 0});

        String requestLine = readLine(in);
        String header;
        do {
            header = readLine(in);
        } while (!header.isEmpty());
        out.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return new Connection(username, password, host, port, requestLine);
    }

    private static String readString(DataInputStream in) throws IOException {
        byte[] bytes = new byte[in.readUnsignedByte()];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static String readLine(DataInputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        int b;
        while ((b = in.read()) != '\n') {
            if (b == -1) {
                throw new EOFException("The client closed the connection inside an HTTP line");
            }
            if (b != '\r') {
                line.append((char) b);
            }
        }
        return line.toString();
    }
}
