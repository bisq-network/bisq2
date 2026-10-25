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

package bisq.network.tor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TorSocksProxyFactoryTest {
    private static final String HOST = "abcdefghijklmnopqrstuvwxyz234567abcdefghijklmnopqrstuv.onion";
    private static final int PORT = 1000;
    private static final int TIMEOUT = 5000;
    private static final byte[] IPV4_ADDRESS = {1, 0, 0, 0, 0};

    private final ServerSocket serverSocket;
    private final TorSocksProxyFactory factory;

    // What the fake proxy received. clientClosed is true if the client closed the connection after the last reply.
    private record Request(String username, String host, int port, boolean clientClosed) {
    }

    // How the fake proxy answers. A null value ends the handshake at that step.
    private record Answers(Byte method, Byte authenticationStatus, byte[] reply) {
        static Answers success() {
            return new Answers((byte) 2, (byte) 0, connectReply((byte) 0, IPV4_ADDRESS));
        }
    }

    TorSocksProxyFactoryTest() throws IOException {
        serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        factory = new TorSocksProxyFactory(serverSocket.getLocalPort());
    }

    @AfterEach
    void tearDown() throws IOException {
        serverSocket.close();
    }

    @Test
    void connectsToHostAndPortThroughTheProxy() throws Exception {
        CompletableFuture<Request> request = serve(Answers.success());

        try (Socket socket = factory.getSocket(HOST, PORT, "node1", TIMEOUT)) {
            assertTrue(socket.isConnected());
            // The established connection has no other timeout for its reads
            assertEquals(TIMEOUT, socket.getSoTimeout());
            assertArrayEquals("ok".getBytes(StandardCharsets.US_ASCII), readTwoBytes(socket));
        }
        Request received = request.get(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(HOST, received.host());
        assertEquals(PORT, received.port());
    }

    @Test
    void usernameIsDerivedFromTheStreamId() throws Exception {
        String node1 = usernameOf("node1");
        String node1Again = usernameOf("node1");
        String node2 = usernameOf("node2");

        assertEquals(node1, node1Again);
        assertNotEquals(node1, node2);
        assertFalse(node1.contains("node1"));
    }

    @Test
    void replyWithDomainNameOrIpv6AddressIsRead() throws Exception {
        byte[] domainName = {3, 4, 'h', 'o', 's', 't'};
        byte[] ipv6Address = new byte[17];
        ipv6Address[0] = 4;
        for (byte[] boundAddress : new byte[][]{domainName, ipv6Address}) {
            CompletableFuture<Request> request = serve(new Answers((byte) 2, (byte) 0,
                    connectReply((byte) 0, boundAddress)));
            try (Socket socket = factory.getSocket(HOST, PORT, "node1", TIMEOUT)) {
                assertArrayEquals("ok".getBytes(StandardCharsets.US_ASCII), readTwoBytes(socket));
            }
            request.get(TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    // 0xFF: the proxy accepts none of the offered methods. 0: the proxy selects no authentication, which the node did not
    // offer. clientClosed shows that the node sent no connect request without credentials.
    @Test
    void proxyWithoutUsernameAuthenticationIsRejected() throws Exception {
        for (byte method : new byte[]{(byte) 0xFF, 0}) {
            CompletableFuture<Request> request = serve(new Answers(method, null, null));

            assertThrows(SocketException.class, () -> factory.getSocket(HOST, PORT, "node1", TIMEOUT));
            assertTrue(request.get(TIMEOUT, TimeUnit.MILLISECONDS).clientClosed());
        }
    }

    @Test
    void rejectedUsernameIsReported() throws Exception {
        CompletableFuture<Request> request = serve(new Answers((byte) 2, (byte) 1, null));

        assertThrows(SocketException.class, () -> factory.getSocket(HOST, PORT, "node1", TIMEOUT));
        assertTrue(request.get(TIMEOUT, TimeUnit.MILLISECONDS).clientClosed());
    }

    // The JDK SOCKS client reported an unreachable peer as SocketException. ConnectException is a subclass of it,
    // so code which handles SocketException handles an unreachable peer as before.
    @Test
    void unreachableHostIsReportedAsConnectException() throws Exception {
        CompletableFuture<Request> request = serve(new Answers((byte) 2, (byte) 0,
                connectReply((byte) 4, IPV4_ADDRESS)));

        ConnectException exception = assertThrows(ConnectException.class,
                () -> factory.getSocket(HOST, PORT, "node1", TIMEOUT));
        assertEquals("SOCKS: Host unreachable", exception.getMessage());
        assertTrue(request.get(TIMEOUT, TimeUnit.MILLISECONDS).clientClosed());
    }

    @Test
    void invalidReplyIsReported() throws Exception {
        byte[] wrongVersion = {4, 0, 0, 1, 0, 0, 0, 0, 0, 0};
        byte[] unknownAddressType = {5, 0, 0, 9};
        for (byte[] reply : new byte[][]{wrongVersion, unknownAddressType}) {
            CompletableFuture<Request> request = serve(new Answers((byte) 2, (byte) 0, reply));

            assertThrows(SocketException.class, () -> factory.getSocket(HOST, PORT, "node1", TIMEOUT));
            assertTrue(request.get(TIMEOUT, TimeUnit.MILLISECONDS).clientClosed());
        }
    }

    @Test
    void proxyWhichClosesTheConnectionIsReported() throws Exception {
        CompletableFuture<Request> request = serve(new Answers((byte) 2, (byte) 0, new byte[0]));

        assertThrows(EOFException.class, () -> factory.getSocket(HOST, PORT, "node1", TIMEOUT));
        request.get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Test
    void proxyWhichDoesNotReplyTimesOut() throws Exception {
        CompletableFuture<Request> request = serve(new Answers((byte) 2, (byte) 0, null));

        assertThrows(SocketTimeoutException.class, () -> factory.getSocket(HOST, PORT, "node1", 300));
        assertTrue(request.get(TIMEOUT, TimeUnit.MILLISECONDS).clientClosed());
    }

    @Test
    void invalidArgumentsAreRejectedBeforeConnecting() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> factory.getSocket("", PORT, "node1", TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> factory.getSocket("höst.onion", PORT, "node1", TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> factory.getSocket("a".repeat(256), PORT, "node1", TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> factory.getSocket(HOST, 0, "node1", TIMEOUT));
        assertThrows(IllegalArgumentException.class, () -> factory.getSocket(HOST, 65536, "node1", TIMEOUT));
        assertThrows(NullPointerException.class, () -> factory.getSocket(HOST, PORT, null, TIMEOUT));

        serverSocket.setSoTimeout(200);
        assertThrows(SocketTimeoutException.class, serverSocket::accept);
    }

    private String usernameOf(String streamId) throws Exception {
        CompletableFuture<Request> request = serve(Answers.success());
        factory.getSocket(HOST, PORT, streamId, TIMEOUT).close();
        return request.get(TIMEOUT, TimeUnit.MILLISECONDS).username();
    }

    private static byte[] readTwoBytes(Socket socket) throws IOException {
        byte[] data = new byte[2];
        new DataInputStream(socket.getInputStream()).readFully(data);
        return data;
    }

    // A client which closes its socket before it has read all bytes of the proxy, for example after an error reply,
    // can reset the connection instead of ending the stream. On macOS the proxy then reads "Connection reset", on
    // Linux it reads the end of the stream. Both mean that the client closed the connection. A read timeout is not a
    // SocketException and still fails the request.
    private static boolean awaitClientClose(DataInputStream in) throws IOException {
        try {
            return in.read() == -1;
        } catch (SocketException e) {
            return true;
        }
    }

    private static byte[] connectReply(byte reply, byte[] boundAddress) {
        byte[] bytes = new byte[3 + boundAddress.length + 2];
        bytes[0] = 5;
        bytes[1] = reply;
        System.arraycopy(boundAddress, 0, bytes, 3, boundAddress.length);
        return bytes;
    }

    // A SOCKS5 proxy which accepts one connection on its own thread and answers as given. The future completes with
    // what it received, or exceptionally if the client sent something unexpected.
    private CompletableFuture<Request> serve(Answers answers) {
        CompletableFuture<Request> request = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            String username = null;
            String host = null;
            int port = 0;
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(TIMEOUT);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                OutputStream out = socket.getOutputStream();
                assertEquals(5, in.readByte());
                byte[] methods = new byte[in.readUnsignedByte()];
                in.readFully(methods);
                assertArrayEquals(new byte[]{2}, methods);
                out.write(new byte[]{5, answers.method()});
                out.flush();

                if (answers.method() == 2) {
                    assertEquals(1, in.readByte());
                    byte[] usernameBytes = new byte[in.readUnsignedByte()];
                    in.readFully(usernameBytes);
                    username = new String(usernameBytes, StandardCharsets.US_ASCII);
                    byte[] password = new byte[in.readUnsignedByte()];
                    in.readFully(password);
                    assertArrayEquals(new byte[]{'0'}, password);
                    out.write(new byte[]{1, answers.authenticationStatus()});
                    out.flush();

                    if (answers.authenticationStatus() == 0) {
                        assertEquals(5, in.readByte());
                        assertEquals(1, in.readByte());
                        assertEquals(0, in.readByte());
                        assertEquals(3, in.readByte());
                        byte[] hostBytes = new byte[in.readUnsignedByte()];
                        in.readFully(hostBytes);
                        host = new String(hostBytes, StandardCharsets.US_ASCII);
                        port = in.readUnsignedShort();
                        if (answers.reply() != null) {
                            if (answers.reply().length == 0) {
                                // Closes the connection instead of replying
                                request.complete(new Request(username, host, port, false));
                                return;
                            }
                            // Writes the reply and the data after it at once. The client can close the connection
                            // as soon as it has read the reply, and a later write would then fail.
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bytes.write(answers.reply());
                            if (answers.reply()[0] == 5 && answers.reply()[1] == 0) {
                                bytes.write("ok".getBytes(StandardCharsets.US_ASCII));
                            }
                            out.write(bytes.toByteArray());
                            out.flush();
                        }
                    }
                }
                // Waits until the client closes the connection, or until the read times out
                request.complete(new Request(username, host, port, awaitClientClose(in)));
            } catch (IOException | AssertionError e) {
                request.completeExceptionally(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        return request;
    }
}
