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
    private record Connection(String username, String password, String host, int port, List<String> requestLines) {
    }

    TorHttpClientTest() throws IOException {
    }

    @AfterEach
    void tearDown() throws IOException {
        serverSocket.close();
    }

    @Test
    void eachRequestUsesItsOwnSocksCredentials() throws Exception {
        CompletableFuture<List<Connection>> connections = serve();
        TorHttpClient client = createClient();

        assertEquals("ok", client.get("first", Optional.empty()));
        assertEquals("ok", client.get("second", Optional.empty()));

        List<Connection> received = stopAndGet(connections);
        assertEquals(2, received.size());
        Connection first = received.get(0);
        Connection second = received.get(1);
        assertEquals(HOST, first.host());
        assertEquals(80, first.port());
        assertEquals(List.of("GET /first HTTP/1.1"), first.requestLines());
        assertEquals(List.of("GET /second HTTP/1.1"), second.requestLines());
        assertEquals("0", first.password());
        assertNotEquals(first.username(), second.username());
    }

    // A connection reused for the redirect would put both requests on one Tor circuit (the rules for HTTP requests
    // over Tor in docs/specifications/network/tor-stream-isolation.md)
    @Test
    void sameHostRedirectUsesNewConnection() throws Exception {
        CompletableFuture<List<Connection>> connections = serve();
        TorHttpClient client = createClient();

        assertEquals("ok", client.get("redirect", Optional.empty()));

        List<Connection> received = stopAndGet(connections);
        assertEquals(List.of(List.of("GET /redirect HTTP/1.1"), List.of("GET /target HTTP/1.1")),
                received.stream().map(Connection::requestLines).toList());
        assertNotEquals(received.get(0).username(), received.get(1).username());
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

    // When a request has returned, the client has opened and closed all its connections, so the proxy can stop
    private List<Connection> stopAndGet(CompletableFuture<List<Connection>> connections) throws Exception {
        serverSocket.close();
        return connections.get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    // A SOCKS5 proxy which accepts connections one after the other until the server socket is closed. On each
    // connection it performs the SOCKS5 handshake. Then it answers HTTP requests and keeps the connection open until
    // the client closes it. It answers "GET /redirect" with a redirect to "/target" on the same host, and any other
    // request with "ok". The future completes with what it received.
    private CompletableFuture<List<Connection>> serve() {
        CompletableFuture<List<Connection>> result = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            List<Connection> connections = new ArrayList<>();
            try {
                while (true) {
                    Socket socket;
                    try {
                        socket = serverSocket.accept();
                    } catch (SocketException e) {
                        if (serverSocket.isClosed()) {
                            break;
                        }
                        throw e;
                    }
                    try (socket) {
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

        // No "Connection: close": the client must not send a second request on this connection
        List<String> requestLines = new ArrayList<>();
        String requestLine;
        while ((requestLine = readRequestLine(in)) != null) {
            requestLines.add(requestLine);
            String header;
            do {
                header = readLine(in);
                if (header == null) {
                    throw new EOFException("The client closed the connection inside an HTTP request");
                }
            } while (!header.isEmpty());
            String response = requestLine.startsWith("GET /redirect ")
                    ? "HTTP/1.1 302 Found\r\nLocation: http://" + HOST + "/target\r\nContent-Length: 0\r\n\r\n"
                    : "HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok";
            out.write(response.getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }
        return new Connection(username, password, host, port, requestLines);
    }

    // Returns null if the client closed the connection instead of sending another request
    private static String readRequestLine(DataInputStream in) throws IOException {
        try {
            return readLine(in);
        } catch (SocketException e) {
            return null; // The client may close the connection with a reset
        }
    }

    private static String readString(DataInputStream in) throws IOException {
        byte[] bytes = new byte[in.readUnsignedByte()];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    // Returns null if the client closed the connection before the first byte of the line
    private static String readLine(DataInputStream in) throws IOException {
        StringBuilder line = new StringBuilder();
        int b;
        while ((b = in.read()) != '\n') {
            if (b == -1) {
                if (line.isEmpty()) {
                    return null;
                }
                throw new EOFException("The client closed the connection inside an HTTP line");
            }
            if (b != '\r') {
                line.append((char) b);
            }
        }
        return line.toString();
    }
}
