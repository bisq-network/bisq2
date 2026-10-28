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

import bisq.common.util.ExceptionUtil;
import bisq.network.http.utils.ResponseBodyTooLargeException;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TorHttpClientTest {
    private static final String HOST = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuvwx.onion";
    private static final int TIMEOUT = 10_000;

    // The client connects to 127.0.0.1. getLoopbackAddress() returns ::1 when the JVM prefers IPv6.
    private final ServerSocket serverSocket = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.1"));

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
        Socks5ProxyProvider proxyProvider = new Socks5ProxyProvider(new Socks5Proxy("127.0.0.1", serverSocket.getLocalPort()));
        TorHttpClient client = new TorHttpClient("http://" + HOST, "http://" + HOST, "test", 1024, proxyProvider);

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

    // The fake proxy answers "ok", two bytes, so a client which reads at most one byte fails the request
    @Test
    void responseBodyAboveTheLimitFailsTheRequest() throws Exception {
        CompletableFuture<List<Connection>> connections = serve(1);
        Socks5ProxyProvider proxyProvider = new Socks5ProxyProvider(new Socks5Proxy("127.0.0.1", serverSocket.getLocalPort()));
        TorHttpClient client = new TorHttpClient("http://" + HOST, "http://" + HOST, "test", 1, proxyProvider);

        IOException thrown = assertThrows(IOException.class, () -> client.get("first", Optional.empty()));

        assertInstanceOf(ResponseBodyTooLargeException.class, ExceptionUtil.getRootCause(thrown));
        connections.get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    // Apache reads the rest of a body when it closes the response, to reuse the connection. Above the limit the client
    // closes the connection instead, so that a provider cannot keep the request busy with an endless body.
    @Test
    void responseBodyAboveTheLimitEndsTheConnectionWithoutReadingTheRest() throws Exception {
        long declaredLength = 256L * 1024 * 1024;
        CompletableFuture<Long> numBytesSent = serveBody(declaredLength);
        Socks5ProxyProvider proxyProvider = new Socks5ProxyProvider(new Socks5Proxy("127.0.0.1", serverSocket.getLocalPort()));
        TorHttpClient client = new TorHttpClient("http://" + HOST, "http://" + HOST, "test", 1024, proxyProvider);

        IOException thrown = assertThrows(IOException.class, () -> client.get("first", Optional.empty()));

        assertInstanceOf(ResponseBodyTooLargeException.class, ExceptionUtil.getRootCause(thrown));
        // The socket buffers take a few MB. Had the client read the rest, the proxy would have sent the whole body.
        assertTrue(numBytesSent.get(TIMEOUT, TimeUnit.MILLISECONDS) < 64L * 1024 * 1024);
    }

    // shutdown() can run after the request checked for it and before the request stored its HTTP client, so shutdown()
    // finds no client to close. The request must then end without a connection.
    @Test
    void requestWhichMissedTheShutdownEndsWithoutAConnection() throws Exception {
        CompletableFuture<List<Connection>> connections = serve(1);
        Socks5Proxy proxy = new Socks5Proxy("127.0.0.1", serverSocket.getLocalPort());
        AtomicReference<TorHttpClient> clientReference = new AtomicReference<>();
        Socks5ProxyProvider proxyProvider = mock(Socks5ProxyProvider.class);
        // The request asks for the proxy between its check of the shutdown and the creation of its client
        when(proxyProvider.getSocks5Proxy()).thenAnswer(invocation -> {
            assertTrue(clientReference.get().shutdown().get(TIMEOUT, TimeUnit.MILLISECONDS));
            return proxy;
        });
        TorHttpClient client = new TorHttpClient("http://" + HOST, "http://" + HOST, "test", 1024, proxyProvider);
        clientReference.set(client);

        assertEquals("", client.get("first", Optional.empty()));
        assertFalse(connections.isDone());
    }

    // The shutdown closes the client of a running request on an executor of its own, and shuts the executor down, so
    // that its thread ends at once. On JDK 21 an executor which is not shut down keeps its thread until a garbage
    // collection finds the executor unreachable, so a collection within the wait can hide a missing shutdown.
    @Test
    void shutdownEndsTheThreadWhichClosesTheClient() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        Thread proxyThread = new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                connected.countDown();
                // Never answers, so the request waits until the shutdown closes its client
                byte[] buffer = new byte[256];
                while (socket.getInputStream().read(buffer) != -1) {
                }
            } catch (IOException ignore) {
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();
        Socks5ProxyProvider proxyProvider = new Socks5ProxyProvider(new Socks5Proxy("127.0.0.1", serverSocket.getLocalPort()));
        TorHttpClient client = new TorHttpClient("http://" + HOST, "http://" + HOST, "test", 1024, proxyProvider);
        CompletableFuture<String> request = CompletableFuture.supplyAsync(() -> {
            try {
                return client.get("first", Optional.empty());
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
        assertTrue(connected.await(TIMEOUT, TimeUnit.MILLISECONDS));

        assertTrue(client.shutdown().get(TIMEOUT, TimeUnit.MILLISECONDS));

        // The request ends with an error or without a response once its client is closed
        request.handle((result, throwable) -> null).get(TIMEOUT, TimeUnit.MILLISECONDS);
        long deadline = System.currentTimeMillis() + 1000;
        while (isShutdownThreadAlive() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertFalse(isShutdownThreadAlive());
    }

    private static boolean isShutdownThreadAlive() {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> thread.getName().equals("TorHttpClient-shutdown") && thread.isAlive());
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

    // A SOCKS5 proxy for one connection which answers with a body of the declared length and writes it until the
    // client closes the connection. The future completes with the number of bytes of the body which it wrote.
    private CompletableFuture<Long> serveBody(long declaredLength) {
        CompletableFuture<Long> result = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            long numBytesSent = 0;
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(TIMEOUT);
                readRequest(socket);
                OutputStream out = socket.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\nContent-Length: " + declaredLength + "\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                byte[] chunk = new byte[8192];
                Arrays.fill(chunk, (byte) 'a');
                while (numBytesSent < declaredLength) {
                    out.write(chunk);
                    numBytesSent += chunk.length;
                }
            } catch (IOException | RuntimeException | AssertionError e) {
                // The client closed the connection
            }
            result.complete(numBytesSent);
        });
        thread.setDaemon(true);
        thread.start();
        return result;
    }

    private static Connection handle(Socket socket) throws IOException {
        Connection connection = readRequest(socket);
        OutputStream out = socket.getOutputStream();
        out.write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return connection;
    }

    // Performs the SOCKS5 handshake and reads the HTTP request
    private static Connection readRequest(Socket socket) throws IOException {
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
