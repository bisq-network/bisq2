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

package bisq.api.web_socket.compression;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the filter through a real Grizzly listener with a raw socket as the client, so that the parts
 * the unit tests cannot reach are covered: where the add-on installs the filter, that the handshake
 * response carries the negotiated extension, and that frames survive the round trip through the
 * {@code WebSocketFilter}, which rejects anything with a reserved bit still set.
 */
class PerMessageDeflateIntegrationTest {
    private static final String PATH = "/websocket-compression-test";
    private static final String OFFER = "permessage-deflate";
    private static final String NEGOTIATED =
            "permessage-deflate; client_no_context_takeover; server_no_context_takeover";
    // Long and repetitive, so that both directions are above MIN_SIZE_TO_COMPRESS and actually shrink
    private static final String MESSAGE = "{\"topic\":\"OFFERS\",\"payload\":\"abcdefghij\"}".repeat(10);

    // Asks the echo application to answer with a fragmented message instead of a single frame
    private static final String FRAGMENT_COMMAND = "fragment-the-reply";

    // A version the server does not speak, so the upgrade is refused before it can succeed
    private static final String UNSUPPORTED_VERSION = "8";

    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_CLOSE = 0x8;

    private HttpServer server;
    private EchoApplication application;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        port = findFreePort();
        server = new HttpServer();
        NetworkListener listener = new NetworkListener("test", "127.0.0.1", port);
        listener.registerAddOn(new WebSocketAddOn());
        listener.registerAddOn(new PerMessageDeflateAddOn());
        server.addListener(listener);

        application = new EchoApplication();
        WebSocketEngine.getEngine().register("", PATH, application);
        server.start();
    }

    @AfterEach
    void tearDown() {
        WebSocketEngine.getEngine().unregister(application);
        server.shutdownNow();
    }

    @Test
    void installsTheFilterDirectlyBelowTheWebSocketFilter() {
        FilterChainBuilder builder = FilterChainBuilder.stateless()
                .add(new TransportFilter())
                .add(new WebSocketFilter());

        new PerMessageDeflateAddOn().setup(null, builder);

        List<Class<?>> types = builder.build().stream().map(Object::getClass).toList();
        assertThat(types).containsExactly(TransportFilter.class, PerMessageDeflateFilter.class, WebSocketFilter.class);
    }

    @Test
    void failsFastIfThereIsNoWebSocketFilterToInstallBelow() {
        FilterChainBuilder builder = FilterChainBuilder.stateless().add(new TransportFilter());

        assertThatThrownBy(() -> new PerMessageDeflateAddOn().setup(null, builder))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void compressesInBothDirections() throws Exception {
        try (Client client = new Client(port)) {
            assertThat(client.handshake(OFFER).get("sec-websocket-extensions")).isEqualTo(NEGOTIATED);

            byte[] compressed = clientDeflate(MESSAGE.getBytes(StandardCharsets.UTF_8));
            assertThat(compressed.length).isLessThan(MESSAGE.length());
            client.send(OPCODE_TEXT, true, compressed);

            Frame echo = client.read();
            assertThat(echo.opcode()).isEqualTo(OPCODE_TEXT);
            assertThat(echo.rsv1()).isTrue();
            assertThat(echo.payload().length).isLessThan(MESSAGE.length());
            assertThat(new String(clientInflate(echo.payload()), StandardCharsets.UTF_8)).isEqualTo(MESSAGE);
        }
    }

    @Test
    void acceptsAnUncompressedFrameOnACompressedConnection() throws Exception {
        try (Client client = new Client(port)) {
            client.handshake(OFFER);

            client.send(OPCODE_TEXT, false, MESSAGE.getBytes(StandardCharsets.UTF_8));

            assertThat(textOf(client.read())).isEqualTo(MESSAGE);
        }
    }

    @Test
    void reassemblesACompressedMessageSentAsFragments() throws Exception {
        try (Client client = new Client(port)) {
            client.handshake(OFFER);

            byte[] compressed = clientDeflate(MESSAGE.getBytes(StandardCharsets.UTF_8));
            int split = compressed.length / 2;
            client.sendFrame(OPCODE_TEXT, true, false, Arrays.copyOfRange(compressed, 0, split));
            client.sendFrame((byte) 0x0, false, true, Arrays.copyOfRange(compressed, split, compressed.length));

            assertThat(textOf(client.read())).isEqualTo(MESSAGE);
        }
    }

    @Test
    void neverCompressesAFragmentedOutboundMessage() throws Exception {
        // Several client stacks fail to inflate a compressed message split over several frames, so a
        // fragmented reply has to go out uncompressed even on a compressed connection
        try (Client client = new Client(port)) {
            client.handshake(OFFER);

            client.send(OPCODE_TEXT, false, FRAGMENT_COMMAND.getBytes(StandardCharsets.UTF_8));

            Frame first = client.read();
            assertThat(first.fin()).isFalse();
            assertThat(first.rsv1()).isFalse();
            Frame last = client.read();
            assertThat(last.fin()).isTrue();
            assertThat(last.rsv1()).isFalse();
            assertThat(new String(first.payload(), StandardCharsets.UTF_8)
                    + new String(last.payload(), StandardCharsets.UTF_8)).isEqualTo(MESSAGE);
        }
    }

    @Test
    void picksUpAnOfferSentOnASecondHeaderLine() throws Exception {
        try (Client client = new Client(port)) {
            Map<String, String> headers = client.handshake("permessage-deflate; server_max_window_bits=10", OFFER);

            assertThat(headers.get("sec-websocket-extensions")).isEqualTo(NEGOTIATED);
        }
    }

    @Test
    void leavesTheConnectionUncompressedWhenTheClientDoesNotOffer() throws Exception {
        try (Client client = new Client(port)) {
            assertThat(client.handshake()).doesNotContainKey("sec-websocket-extensions");

            client.send(OPCODE_TEXT, false, MESSAGE.getBytes(StandardCharsets.UTF_8));

            Frame echo = client.read();
            assertThat(echo.rsv1()).isFalse();
            assertThat(new String(echo.payload(), StandardCharsets.UTF_8)).isEqualTo(MESSAGE);
        }
    }

    @Test
    void leavesTheConnectionUncompressedWhenItCannotHonourTheOffer() throws Exception {
        try (Client client = new Client(port)) {
            assertThat(client.handshake("permessage-deflate; server_max_window_bits=10"))
                    .doesNotContainKey("sec-websocket-extensions");

            client.send(OPCODE_TEXT, false, MESSAGE.getBytes(StandardCharsets.UTF_8));

            assertThat(client.read().rsv1()).isFalse();
        }
    }

    /**
     * Pins why {@link PerMessageDeflateHandshakeStateTest#forgetsTheOfferOfAnUpgradeThatFailed()} has to
     * drive the filter directly: a failed upgrade takes the connection with it, so a retry carrying no
     * offer cannot be sent over it and the state left behind is not observable from out here. Should
     * Grizzly ever keep the connection open, this fails and that assumption is worth revisiting.
     */
    @Test
    void closesTheConnectionWhenTheUpgradeFails() throws Exception {
        try (Client client = new Client(port)) {
            assertThat(client.attemptFailingHandshake(PATH, OFFER)).doesNotContain("101");

            assertThatThrownBy(client::handshake).isInstanceOf(IOException.class);
        }
    }

    @Test
    void closesWithTooBigOnAnOversizedFrame() throws Exception {
        try (Client client = new Client(port)) {
            client.handshake(OFFER);

            // Header only: the declared length alone has to be enough to be rejected
            client.write(new byte[]{(byte) (0x80 | OPCODE_TEXT), (byte) (0x80 | 127),
                    0, 0, 0, 0, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});

            Frame close = client.read();
            assertThat(close.opcode()).isEqualTo(OPCODE_CLOSE);
            assertThat(closeCodeOf(close)).isEqualTo(1009);
        }
    }

    @Test
    void closesWithAProtocolErrorOnALengthWithTheMostSignificantBitSet() throws Exception {
        try (Client client = new Client(port)) {
            client.handshake(OFFER);

            // RFC 6455 requires the most significant bit of a 64 bit length to be 0, so this is a
            // malformed frame rather than one that is merely larger than we accept
            client.write(new byte[]{(byte) (0x80 | OPCODE_TEXT), (byte) (0x80 | 127),
                    (byte) 0x80, 0, 0, 0, 0, 0, 0, 0});

            Frame close = client.read();
            assertThat(close.opcode()).isEqualTo(OPCODE_CLOSE);
            assertThat(closeCodeOf(close)).isEqualTo(1002);
        }
    }

    @Test
    void closesWithAProtocolErrorOnACompressedControlFrame() throws Exception {
        try (Client client = new Client(port)) {
            client.handshake(OFFER);

            client.send((byte) 0x9, true, new byte[0]);

            Frame close = client.read();
            assertThat(close.opcode()).isEqualTo(OPCODE_CLOSE);
            assertThat(closeCodeOf(close)).isEqualTo(1002);
        }
    }

    // Server

    private static class EchoApplication extends WebSocketApplication {
        @Override
        public void onMessage(WebSocket socket, String text) {
            if (FRAGMENT_COMMAND.equals(text)) {
                int half = MESSAGE.length() / 2;
                socket.stream(false, MESSAGE.substring(0, half));
                socket.stream(true, MESSAGE.substring(half));
            } else {
                socket.send(text);
            }
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String textOf(Frame frame) throws Exception {
        byte[] payload = frame.rsv1() ? clientInflate(frame.payload()) : frame.payload();
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static int closeCodeOf(Frame frame) {
        return (frame.payload()[0] & 0xFF) << 8 | frame.payload()[1] & 0xFF;
    }

    // Client
    //
    // Deliberately does not reuse the filter's own codec, so that the round trip is verified against an
    // independent implementation of RFC 7692 rather than against itself.

    private record Frame(byte opcode, boolean rsv1, boolean fin, byte[] payload) {
    }

    private static byte[] clientDeflate(byte[] payload) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try {
            deflater.setInput(payload);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            while (!deflater.finished()) {
                out.write(chunk, 0, deflater.deflate(chunk));
            }
            // A finished raw deflate stream ends with a final block, which the peer's inflater accepts
            // as it is, so there is no tail to strip here.
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] clientInflate(byte[] compressed) throws Exception {
        Inflater inflater = new Inflater(true);
        try {
            byte[] withTail = Arrays.copyOf(compressed, compressed.length + 4);
            withTail[compressed.length + 2] = (byte) 0xFF;
            withTail[compressed.length + 3] = (byte) 0xFF;
            inflater.setInput(withTail);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];
            int count;
            while ((count = inflater.inflate(chunk)) > 0) {
                out.write(chunk, 0, count);
            }
            return out.toByteArray();
        } finally {
            inflater.end();
        }
    }

    private static class Client implements AutoCloseable {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private final SecureRandom random = new SecureRandom();

        Client(int port) throws IOException {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            socket.setSoTimeout(10000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
        }

        Map<String, String> handshake(String... extensionOffers) throws IOException {
            write(handshakeRequest(PATH, extensionOffers));
            List<String> lines = readHeaderLines();
            String statusLine = lines.remove(0);
            if (!statusLine.contains("101")) {
                throw new IOException("Handshake failed: " + statusLine);
            }
            return parseHeaders(lines);
        }

        /**
         * Sends an upgrade request that cannot succeed, by naming a protocol version the server does
         * not speak.
         *
         * @return the status line of the response.
         */
        String attemptFailingHandshake(String path, String... extensionOffers) throws IOException {
            write(handshakeRequest(path, UNSUPPORTED_VERSION, extensionOffers));
            List<String> lines = readHeaderLines();
            String statusLine = lines.remove(0);
            drainBody(parseHeaders(lines));
            return statusLine;
        }

        private byte[] handshakeRequest(String path, String... extensionOffers) {
            return handshakeRequest(path, "13", extensionOffers);
        }

        private byte[] handshakeRequest(String path, String version, String... extensionOffers) {
            byte[] key = new byte[16];
            random.nextBytes(key);
            StringBuilder request = new StringBuilder()
                    .append("GET ").append(path).append(" HTTP/1.1\r\n")
                    .append("Host: 127.0.0.1\r\n")
                    .append("Upgrade: websocket\r\n")
                    .append("Connection: Upgrade\r\n")
                    .append("Sec-WebSocket-Version: ").append(version).append("\r\n")
                    .append("Sec-WebSocket-Key: ").append(Base64.getEncoder().encodeToString(key)).append("\r\n");
            for (String offer : extensionOffers) {
                request.append("Sec-WebSocket-Extensions: ").append(offer).append("\r\n");
            }
            return request.append("\r\n").toString().getBytes(StandardCharsets.US_ASCII);
        }

        private List<String> readHeaderLines() throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while (!endsWithBlankLine(buffer.toByteArray())) {
                buffer.write(readByte());
            }
            return new ArrayList<>(Arrays.asList(buffer.toString(StandardCharsets.US_ASCII).split("\r\n")));
        }

        private Map<String, String> parseHeaders(List<String> lines) {
            Map<String, String> headers = new HashMap<>();
            for (String line : lines) {
                int separator = line.indexOf(':');
                if (separator > 0) {
                    headers.put(line.substring(0, separator).trim().toLowerCase(Locale.ROOT),
                            line.substring(separator + 1).trim());
                }
            }
            return headers;
        }

        /**
         * Reads the error body away, so that it is not mistaken for the start of the next response.
         */
        private void drainBody(Map<String, String> headers) throws IOException {
            int length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
            for (int i = 0; i < length; i++) {
                readByte();
            }
        }

        private static boolean endsWithBlankLine(byte[] bytes) {
            int length = bytes.length;
            return length >= 4 && bytes[length - 4] == '\r' && bytes[length - 3] == '\n'
                    && bytes[length - 2] == '\r' && bytes[length - 1] == '\n';
        }

        void send(byte opcode, boolean rsv1, byte[] payload) throws IOException {
            sendFrame(opcode, rsv1, true, payload);
        }

        void sendFrame(byte opcode, boolean rsv1, boolean fin, byte[] payload) throws IOException {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write((fin ? 0x80 : 0x00) | (rsv1 ? 0x40 : 0x00) | opcode);
            if (payload.length <= 125) {
                frame.write(0x80 | payload.length);
            } else {
                frame.write(0x80 | 126);
                frame.write(payload.length >>> 8);
                frame.write(payload.length);
            }
            byte[] mask = new byte[4];
            random.nextBytes(mask);
            frame.write(mask, 0, mask.length);
            for (int i = 0; i < payload.length; i++) {
                frame.write(payload[i] ^ mask[i % 4]);
            }
            write(frame.toByteArray());
        }

        Frame read() throws IOException {
            int firstByte = readByte();
            int secondByte = readByte();
            boolean fin = (firstByte & 0x80) != 0;
            boolean rsv1 = (firstByte & 0x40) != 0;
            byte opcode = (byte) (firstByte & 0x0F);
            boolean masked = (secondByte & 0x80) != 0;
            int length = secondByte & 0x7F;
            if (length == 126) {
                length = readByte() << 8 | readByte();
            } else if (length == 127) {
                long extended = 0;
                for (int i = 0; i < 8; i++) {
                    extended = extended << 8 | readByte();
                }
                length = (int) extended;
            }
            byte[] mask = masked ? readBytes(4) : null;
            byte[] payload = readBytes(length);
            if (mask != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i % 4];
                }
            }
            return new Frame(opcode, rsv1, fin, payload);
        }

        void write(byte[] bytes) throws IOException {
            out.write(bytes);
            out.flush();
        }

        private int readByte() throws IOException {
            int value = in.read();
            if (value < 0) {
                throw new IOException("Connection closed by the server");
            }
            return value;
        }

        private byte[] readBytes(int count) throws IOException {
            byte[] bytes = new byte[count];
            for (int i = 0; i < count; i++) {
                bytes[i] = (byte) readByte();
            }
            return bytes;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
