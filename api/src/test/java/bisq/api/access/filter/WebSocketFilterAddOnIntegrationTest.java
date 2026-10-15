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

package bisq.api.access.filter;

import bisq.api.ApiConfig;
import bisq.api.HttpServerBootstrapService;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.identity.ClientProfile;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.api.web_socket.util.WebSocketIdentity;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.SimpleWebSocket;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sends upgrade requests through a real Grizzly listener with a raw socket as the client. Grizzly decides which
 * request it upgrades to a WebSocket, so only the real filter chain shows whether a request which our filters let
 * pass opens a WebSocket. The listener registers the add-ons of {@link HttpServerBootstrapService} with compression
 * on, as the shipped configs do.
 *
 * <p>Clients A and V are paired. Only A has a session. A refused request must get its status, the server must close
 * the connection, and no WebSocket may open. An accepted WebSocket must have the identity of its session.
 */
class WebSocketFilterAddOnIntegrationTest {
    private static final String PATH = "/websocket-upgrade-test";
    private static final String CLIENT_A = "client-a";
    private static final String CLIENT_V = "client-v";
    private static final String KEY_HEADER = Headers.SEC_WEBSOCKET_KEY + ": dGhlIHNhbXBsZSBub25jZQ==\r\n";

    private HttpServer server;
    private RecordingApplication application;
    private int port;
    private String sessionIdOfA;

    @AfterEach
    void tearDown() {
        if (application != null) {
            WebSocketEngine.getEngine().unregister(application);
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    // A1
    @Test
    void handshakeWithAValidSessionOpensAWebSocketWithTheClientOfTheSession() throws Exception {
        startServer(true);

        assertThat(statusLineOf(upgradeRequest("GET", KEY_HEADER + sessionHeaders(CLIENT_A, sessionIdOfA))))
                .contains("101");
        assertThat(application.awaitConnect()).isTrue();
        assertThat(application.identity()).contains(CLIENT_A);
    }

    // A2
    @Test
    void handshakeWithoutASessionIsRejected() throws Exception {
        startServer(true);

        assertRefused(upgradeRequest("GET", KEY_HEADER), "401");
    }

    @Test
    void handshakeOpensAWebSocketWhenSessionHandlingIsOff() throws Exception {
        startServer(false);

        assertThat(statusLineOf(upgradeRequest("GET", KEY_HEADER + clientIdHeader(CLIENT_V)))).contains("101");
        assertThat(application.awaitConnect()).isTrue();
        // Without session handling the header is the only identity, as on the REST API
        assertThat(application.identity()).contains(CLIENT_V);
    }

    // A4. Grizzly starts the handshake for every request whose Upgrade value is "websocket" and does not check the
    // method. Without the validation filter this request opens a WebSocket without a session, with the identity
    // of the client which the header names.
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void upgradeWithAnotherMethodThanGetIsRejectedWithoutASession(String method) throws Exception {
        startServer(true);

        assertRefused(upgradeRequest(method, KEY_HEADER + clientIdHeader(CLIENT_V)), "400");
    }

    // A9
    @Test
    void upgradeWithAnotherMethodThanGetIsRejectedWhenSessionHandlingIsOff() throws Exception {
        startServer(false);

        assertRefused(upgradeRequest("POST", KEY_HEADER), "400");
    }

    // A handshake which Grizzly refuses itself, here for a Connection header without "Upgrade". Grizzly answers 400
    // and kept the connection open, and every later byte, also a complete upgrade request of another client, was
    // content of the refused request: Grizzly repeated the handshake with its headers, and the authentication filter
    // let the bytes pass as content of an authenticated request. The server must close the connection after the
    // answer, without waiting for more bytes. If it keeps the connection open, the read times out.
    @Test
    void aHandshakeWhichGrizzlyRefusesClosesTheConnection() throws Exception {
        startServer(true);
        String refused = "GET " + PATH + " HTTP/1.1\r\n"
                + "Host: 127.0.0.1\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: keep-alive\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + KEY_HEADER
                + sessionHeaders(CLIENT_A, sessionIdOfA)
                + "\r\n";

        assertRefused(refused, "400");
    }

    // Grizzly marks a WebSocket closed before it closes the connection, for example when the revocation of a client
    // closes a WebSocket from another thread. A frame which arrived in between started a second handshake with the
    // upgrade request of the closed WebSocket, and the application got a second onConnect with the identity of the
    // first, without a new authentication. The application holds this state with setClosed(), so that the test does not
    // depend on the timing of the close.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void contentAfterTheWebSocketClosedOpensNoSecondWebSocket(boolean supportSessionHandling) throws Exception {
        startServer(supportSessionHandling);
        String identityHeaders = supportSessionHandling
                ? sessionHeaders(CLIENT_A, sessionIdOfA)
                : clientIdHeader(CLIENT_A);

        String afterTheClose;
        try (Socket socket = connect()) {
            write(socket.getOutputStream(), upgradeRequest("GET", KEY_HEADER + identityHeaders));
            InputStream in = socket.getInputStream();
            assertThat(readLine(in)).contains("101");
            skipHeaders(in);
            assertThat(application.awaitConnect()).isTrue();
            writeFrame(socket, RecordingApplication.MARK_CLOSED);
            assertThat(application.awaitMarkedClosed()).isTrue();

            try {
                writeFrame(socket, "after the close");
            } catch (SocketException e) {
                // The server can have closed the connection already
            }
            try {
                afterTheClose = readUntilClosed(in);
            } catch (SocketTimeoutException e) {
                throw new AssertionError("The server kept the connection open. Calls of onConnect: "
                        + application.connectCount(), e);
            }
        }

        assertThat(statusLines(afterTheClose)).isEmpty();
        assertThat(application.connectCount()).isEqualTo(1);
    }

    private void startServer(boolean supportSessionHandling) throws IOException {
        SessionService sessionService = new SessionService(60);
        sessionIdOfA = sessionService.createSession(CLIENT_A).getSessionId();
        PairingService pairingService = mock(PairingService.class);
        for (String clientId : List.of(CLIENT_A, CLIENT_V)) {
            when(pairingService.findClientProfile(clientId))
                    .thenReturn(Optional.of(ClientProfile.fromSecret(clientId, "client-secret", "client-name")));
        }
        SessionAuthenticationService sessionAuthenticationService =
                new SessionAuthenticationService(pairingService, sessionService);

        port = findFreePort();
        server = new HttpServer();
        NetworkListener listener = new NetworkListener("test", "127.0.0.1", port);
        HttpServerBootstrapService.webSocketAddOns(apiConfig(supportSessionHandling), sessionAuthenticationService)
                .forEach(listener::registerAddOn);
        server.addListener(listener);

        application = new RecordingApplication();
        WebSocketEngine.getEngine().register("", PATH, application);
        server.start();
    }

    /**
     * Asserts that the server answers with the given status, writes no other response after it, and closes the
     * connection, and that no WebSocket opened. If the server keeps the connection open, the read times out and
     * the test fails.
     */
    private void assertRefused(String request, String status) throws IOException {
        try (Socket socket = connect()) {
            write(socket.getOutputStream(), request);
            InputStream in = socket.getInputStream();
            assertThat(readLine(in))
                    .as(() -> "status line. Identity of a WebSocket which opened: " + identityOfAnOpenedWebSocket())
                    .startsWith("HTTP/1.1 " + status);
            assertThat(statusLines(readUntilClosed(in))).isEmpty();
        }
        assertThat(application.hasConnected()).isFalse();
    }

    // Only for the message of a failed assertion
    private String identityOfAnOpenedWebSocket() {
        try {
            return application.awaitConnect() ? application.identity().orElse("none") : "no WebSocket opened";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "unknown";
        }
    }

    private String statusLineOf(String request) throws IOException {
        return responseHeadersOf(request).get(0);
    }

    private List<String> responseHeadersOf(String request) throws IOException {
        try (Socket socket = connect()) {
            write(socket.getOutputStream(), request);
            InputStream in = socket.getInputStream();
            List<String> lines = new ArrayList<>();
            String line;
            while (!(line = readLine(in)).isEmpty()) {
                lines.add(line);
            }
            return lines;
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
        socket.setSoTimeout(10000);
        return socket;
    }

    private static void write(OutputStream out, String request) throws IOException {
        out.write(request.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int value;
        while ((value = in.read()) >= 0 && value != '\n') {
            line.write(value);
        }
        if (value < 0 && line.size() == 0) {
            throw new IOException("Connection closed by the server without a response");
        }
        return line.toString(StandardCharsets.US_ASCII).trim();
    }

    private static void skipHeaders(InputStream in) throws IOException {
        while (!readLine(in).isEmpty()) {
            // Skips the headers of the response
        }
    }

    private static void writeFrame(Socket socket, String text) throws IOException {
        socket.getOutputStream().write(maskedTextFrame(text));
        socket.getOutputStream().flush();
    }

    // Reads until the server closes the connection. A reset also ends the connection.
    private static String readUntilClosed(InputStream in) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            int count;
            while ((count = in.read(buffer)) >= 0) {
                response.write(buffer, 0, count);
            }
        } catch (SocketException e) {
            // Reset by the server: the connection is closed as well
        }
        return response.toString(StandardCharsets.US_ASCII);
    }

    // A client masks every frame (RFC 6455, section 5.3)
    private static byte[] maskedTextFrame(String text) {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        byte[] mask = {0x11, 0x22, 0x33, 0x44};
        byte[] frame = new byte[2 + mask.length + payload.length];
        frame[0] = (byte) 0x81;
        frame[1] = (byte) (0x80 | payload.length);
        System.arraycopy(mask, 0, frame, 2, mask.length);
        for (int i = 0; i < payload.length; i++) {
            frame[2 + mask.length + i] = (byte) (payload[i] ^ mask[i % mask.length]);
        }
        return frame;
    }

    private static List<String> statusLines(String response) {
        return Arrays.stream(response.split("\r\n"))
                .filter(line -> line.startsWith("HTTP/"))
                .toList();
    }

    private static String upgradeRequest(String method, String additionalHeaders) {
        return upgradeRequest(method, PATH, "127.0.0.1", additionalHeaders);
    }

    // The headers which Grizzly needs to start a WebSocket handshake, and the given additional header lines
    private static String upgradeRequest(String method, String path, String host, String additionalHeaders) {
        return method + " " + path + " HTTP/1.1\r\n"
                + "Host: " + host + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + "Content-Length: 0\r\n"
                + additionalHeaders
                + "\r\n";
    }

    private static String sessionHeaders(String clientId, String sessionId) {
        return clientIdHeader(clientId) + Headers.SESSION_ID + ": " + sessionId + "\r\n";
    }

    private static String clientIdHeader(String clientId) {
        return Headers.CLIENT_ID + ": " + clientId + "\r\n";
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static ApiConfig apiConfig(boolean supportSessionHandling) {
        return new ApiConfig(ApiAccessTransportType.CLEAR,
                3600,
                false,
                true,
                true,
                true,
                "127.0.0.1",
                8090,
                80,
                supportSessionHandling,
                false,
                false,
                "",
                List.of(),
                false,
                60);
    }

    private static class RecordingApplication extends WebSocketApplication {
        // A frame with this text puts its WebSocket into the state CLOSED and leaves the connection open
        static final String MARK_CLOSED = "mark-closed";

        private final CountDownLatch connected = new CountDownLatch(1);
        private final AtomicInteger connectCount = new AtomicInteger();
        private final CountDownLatch markedClosed = new CountDownLatch(1);
        private final AtomicReference<Optional<String>> identity = new AtomicReference<>(Optional.empty());

        @Override
        public void onConnect(WebSocket socket) {
            super.onConnect(socket);
            identity.set(WebSocketIdentity.findClientId(socket));
            connectCount.incrementAndGet();
            connected.countDown();
        }

        @Override
        public void onMessage(WebSocket socket, String text) {
            if (MARK_CLOSED.equals(text)) {
                ((SimpleWebSocket) socket).setClosed();
                markedClosed.countDown();
            }
        }

        boolean hasConnected() {
            return connected.getCount() == 0;
        }

        int connectCount() {
            return connectCount.get();
        }

        boolean awaitMarkedClosed() throws InterruptedException {
            return markedClosed.await(10, TimeUnit.SECONDS);
        }

        // Grizzly writes the response of the handshake before it calls onConnect
        boolean awaitConnect() throws InterruptedException {
            return connected.await(10, TimeUnit.SECONDS);
        }

        Optional<String> identity() {
            return identity.get();
        }
    }
}
