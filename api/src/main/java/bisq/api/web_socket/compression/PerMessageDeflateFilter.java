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

import bisq.api.access.filter.HttpRequestFilterUtils;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.websockets.ProtocolError;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Adds RFC 7692 {@code permessage-deflate} support to the Grizzly WebSocket stack, which does not
 * implement it on its own (and in fact rejects any frame with a reserved bit set).
 *
 * <p>The filter is installed directly below the {@code WebSocketFilter} and works purely on the wire
 * bytes: inbound it inflates compressed messages and hands plain, complete frames upwards, outbound it
 * deflates the frames the {@code WebSocketFilter} has just serialized. Everything above it therefore
 * sees an uncompressed connection.
 *
 * <p>Outbound, only an unfragmented message is ever compressed: a fragment is forwarded as it is. Several
 * client stacks fail to inflate a compressed message that arrives split over multiple frames, so a
 * fragment that we cannot send as a single compressed frame is sent uncompressed instead. Inbound the
 * opposite holds, as we have to accept whatever a peer sends: fragments are reassembled and inflated.
 *
 * <p>Compression is always negotiated with {@code client_no_context_takeover} and
 * {@code server_no_context_takeover}. Keeping the deflate window across messages would let an attacker
 * who can influence any later message probe secrets sent in an earlier one by observing frame sizes
 * (the CRIME/BREACH attack); resetting per message limits that to data within a single message.
 */
@Slf4j
public class PerMessageDeflateFilter extends BaseFilter {
    public static final String EXTENSION_NAME = "permessage-deflate";

    private static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
    private static final String NEGOTIATED_EXTENSION =
            EXTENSION_NAME + "; client_no_context_takeover; server_no_context_takeover";

    private static final byte OPCODE_CONTINUATION = 0x0;
    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_BINARY = 0x2;
    private static final byte OPCODE_CLOSE = 0x8;
    private static final int MASK_SIZE = 4;
    private static final byte RSV1 = 0x40;

    // RFC 6455 close codes. We handle the frames below the WebSocketFilter, which therefore never learns
    // about the error and cannot close on our behalf, so we state the reason ourselves.
    private static final int CLOSE_PROTOCOL_ERROR = 1002;
    private static final int CLOSE_TOO_BIG = 1009;

    // Inflating attacker-controlled data is a zip-bomb vector, so both the compressed input and the
    // inflated output are bounded. The limits apply to inbound data only: they exist to cap how much
    // heap a peer can make us allocate, not to restrict what we are willing to send.
    private static final int MAX_FRAME_PAYLOAD_SIZE = 10 * 1024 * 1024;
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024;
    // Below this size deflate reliably makes the payload larger, so we do not even try.
    private static final int MIN_SIZE_TO_COMPRESS = 64;
    // The empty deflate block a SYNC_FLUSH emits; RFC 7692 requires it to be stripped before sending.
    private static final byte[] DEFLATE_TAIL = {0x00, 0x00, (byte) 0xFF, (byte) 0xFF};

    private static final Attribute<ConnectionState> STATE =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("bisq.web_socket.permessage_deflate");

    @Override
    public NextAction handleRead(FilterChainContext ctx) {
        Object message = ctx.getMessage();
        if (!(message instanceof HttpContent httpContent)) {
            return ctx.getInvokeAction();
        }

        Connection<?> connection = ctx.getConnection();
        Optional<ConnectionState> state = findState(connection);
        if (state.filter(ConnectionState::isEnabled).isEmpty()) {
            HttpRequestFilterUtils.resolveAsHttpRequest(message)
                    .filter(HttpRequestFilterUtils::isWebsocketHandshakeRequest)
                    .ifPresent(request -> onHandshakeRequest(connection, request));
            return ctx.getInvokeAction();
        }

        Inbound inbound;
        try {
            inbound = processInbound(ctx.getMemoryManager(),
                    Optional.ofNullable(httpContent.getContent()),
                    state.get());
        } catch (ProtocolError e) {
            log.warn("Closing WebSocket connection: {}", e.getMessage());
            closeWithStatus(ctx, e instanceof MessageTooLarge ? CLOSE_TOO_BIG : CLOSE_PROTOCOL_ERROR);
            return ctx.getStopAction();
        }

        if (inbound.passThrough()) {
            return ctx.getInvokeAction();
        }
        return inbound.content()
                .map(content -> {
                    ctx.setMessage(HttpContent.builder(httpContent.getHttpHeader()).content(content).build());
                    return ctx.getInvokeAction();
                })
                .orElseGet(ctx::getStopAction);
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) {
        Optional<ConnectionState> found = findState(ctx.getConnection());
        if (found.isEmpty()) {
            return ctx.getInvokeAction();
        }
        ConnectionState state = found.get();

        Object message = ctx.getMessage();
        if (message instanceof HttpContent httpContent) {
            // The handshake response is the only point where we know the upgrade actually succeeded,
            // so compression is switched on exactly when we confirm the extension to the client.
            if (!state.enabled && httpContent.getHttpHeader() instanceof HttpResponsePacket response) {
                if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101.getStatusCode()) {
                    // Replaces rather than appends: Grizzly may have written the extensions a
                    // WebSocketApplication declares, but it only echoes their names and cannot apply any
                    // of them, so permessage-deflate is the only one this response can honestly confirm.
                    response.setHeader(SEC_WEBSOCKET_EXTENSIONS, NEGOTIATED_EXTENSION);
                    state.enabled = true;
                } else {
                    // The upgrade this offer belonged to failed. Were the state left attached, a retry on
                    // the same connection that does not offer the extension would still be answered with
                    // it, and we would compress frames the client never agreed to decompress.
                    STATE.remove(ctx.getConnection());
                }
            }
            return ctx.getInvokeAction();
        }

        if (state.enabled && message instanceof Buffer frame) {
            ctx.setMessage(deflateFrame(ctx.getMemoryManager(), frame));
        }
        return ctx.getInvokeAction();
    }

    /**
     * Sends a close frame before dropping the connection, so that the peer learns why it was closed
     * instead of only seeing the socket go away. The connection is closed once the frame is out.
     */
    private static void closeWithStatus(FilterChainContext ctx, int closeCode) {
        Connection<?> connection = ctx.getConnection();
        try {
            byte[] payload = {(byte) (closeCode >>> 8), (byte) closeCode};
            Buffer closeFrame = Buffers.wrap(ctx.getMemoryManager(), buildFrame(OPCODE_CLOSE, payload));
            ctx.write(closeFrame, new EmptyCompletionHandler<WriteResult>() {
                @Override
                public void completed(WriteResult result) {
                    connection.closeSilently();
                }

                @Override
                public void failed(Throwable throwable) {
                    connection.closeSilently();
                }
            });
        } catch (Exception e) {
            log.debug("Could not send the close frame", e);
            connection.closeSilently();
        }
    }

    private static Optional<ConnectionState> findState(Connection<?> connection) {
        return Optional.ofNullable(STATE.get(connection));
    }

    // Negotiation

    private void onHandshakeRequest(Connection<?> connection, HttpRequestPacket request) {
        if (findState(connection).isPresent()) {
            // The upgrade request may be re-delivered before the protocol switch completes
            return;
        }
        // A client may spread its offers over several header lines, so all of them have to be looked at
        for (String header : request.getHeaders().values(SEC_WEBSOCKET_EXTENSIONS)) {
            if (Optional.ofNullable(header).filter(PerMessageDeflateFilter::acceptsOffer).isPresent()) {
                STATE.set(connection, new ConnectionState());
                return;
            }
        }
    }

    static boolean acceptsOffer(String header) {
        return parseOffers(header).stream()
                .flatMap(List::stream)
                .anyMatch(offer -> EXTENSION_NAME.equalsIgnoreCase(offer.get(0)) && canHonour(offer));
    }

    /**
     * Splits a header into its offers and every offer into its parts, the extension name followed by its
     * parameters. RFC 6455 lets a parameter value be a quoted string, in which a "," or a ";" is data
     * rather than a separator, so a plain split would read one offer as several and could see an offer
     * the client never made.
     *
     * @return the offers, each with its name in the first position, or empty if the header is malformed,
     * which here means a quote that is never closed
     */
    private static Optional<List<List<String>>> parseOffers(String header) {
        List<List<String>> offers = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < header.length(); i++) {
            char character = header.charAt(i);
            if (escaped) {
                // A quoted pair stands for the character it escapes, but as no parameter we honour has a
                // value that needs escaping, it is kept as written and left for the value checks to refuse
                escaped = false;
            } else if (quoted && character == '\\') {
                escaped = true;
            } else if (character == '"') {
                quoted = !quoted;
            } else if (!quoted && (character == ';' || character == ',')) {
                parts.add(part.toString().trim());
                part.setLength(0);
                if (character == ',') {
                    offers.add(parts);
                    parts = new ArrayList<>();
                }
                continue;
            }
            part.append(character);
        }
        // An escape only ever starts inside a quoted string, so a header ending in one is unclosed as well
        if (quoted) {
            return Optional.empty();
        }
        parts.add(part.toString().trim());
        offers.add(parts);
        return Optional.of(offers);
    }

    private static boolean canHonour(List<String> offerParts) {
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < offerParts.size(); i++) {
            String parameter = offerParts.get(i);
            int separator = parameter.indexOf('=');
            String name = (separator < 0 ? parameter : parameter.substring(0, separator))
                    .trim().toLowerCase(Locale.ROOT);
            Optional<String> value = separator < 0
                    ? Optional.empty()
                    : Optional.of(unquote(parameter.substring(separator + 1).trim()));
            // RFC 7692: an offer repeating a parameter must not be accepted
            if (!seen.add(name)) {
                return false;
            }
            switch (name) {
                case "client_no_context_takeover", "server_no_context_takeover" -> {
                    // We apply both unconditionally, but RFC 7692 has them carry no value
                    if (value.isPresent()) {
                        return false;
                    }
                }
                case "client_max_window_bits" -> {
                    // Limits the window the CLIENT compresses with, and RFC 7692 lets us ignore the
                    // value: our Inflater always uses the largest window, which decodes a stream
                    // produced with any smaller one. Only a value outside the range is refused, as an
                    // offer we cannot make sense of.
                    if (value.filter(bits -> !isWindowBits(bits)).isPresent()) {
                        return false;
                    }
                }
                case "server_max_window_bits" -> {
                    // Limits the window WE compress with, which java.util.zip cannot restrict, so only
                    // the largest can be honoured and RFC 7692 has us decline anything else. The offer
                    // has to carry a value, so one without is refused too.
                    if (value.filter("15"::equals).isEmpty()) {
                        return false;
                    }
                }
                // RFC 7692: an offer carrying an unknown parameter must not be accepted
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true for a window size RFC 7692 allows, which is 8 to 15 without leading zeroes.
     */
    private static boolean isWindowBits(String value) {
        if (value.isEmpty() || value.startsWith("0")) {
            return false;
        }
        try {
            int bits = Integer.parseInt(value);
            return bits >= 8 && bits <= 15;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String unquote(String value) {
        return value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1)
                : value;
    }

    // Inbound

    /**
     * Inflates all complete messages held by {@code input}, retaining any trailing partial frame on
     * {@code state} so that nothing above this filter ever sees an incomplete frame.
     */
    static Inbound processInbound(MemoryManager<?> memoryManager, Optional<Buffer> content, ConnectionState state) {
        Optional<Buffer> remaining = content.filter(Buffer::hasRemaining);
        if (remaining.isEmpty()) {
            return Inbound.nothing();
        }
        Buffer input = remaining.get();

        Optional<ByteArrayOutputStream> pending = state.leftover;
        boolean pristine = pending.isEmpty();
        if (!pristine) {
            ByteArrayOutputStream leftover = pending.get();
            // Appending is amortized constant time, whereas merging and re-parsing the accumulated
            // prefix on every read would be quadratic in the number of reads a peer splits a frame
            // into, which a peer dribbling out a large frame byte by byte can drive on its own.
            leftover.writeBytes(copyBytes(input, input.position(), input.limit()));
            if (leftover.size() < state.leftoverRequiredSize) {
                return Inbound.nothing();
            }
            input = Buffers.wrap(memoryManager, leftover.toByteArray());
            state.leftover = Optional.empty();
            state.leftoverRequiredSize = 0;
        }

        int limit = input.limit();
        int position = input.position();
        List<Buffer> output = new ArrayList<>();
        boolean inflated = false;
        int requiredSize = 0;

        while (position < limit) {
            Frame frame = parseFrame(input, position, limit);
            if (!frame.complete()) {
                requiredSize = frame.end() - position;
                break;
            }
            if (frame.isControl()) {
                output.add(input.slice(position, frame.end()));
            } else {
                // We consume the frames of a compressed message rather than forwarding them, so the
                // WebSocketFilter above never sees them and cannot validate the fragmentation of such
                // a message. Whatever it would have rejected has to be rejected here instead.
                if (frame.opcode() == OPCODE_CONTINUATION) {
                    if (!state.messageOpen) {
                        throw new ProtocolError("Continuation frame without a message to continue");
                    }
                    if (frame.rsv1()) {
                        throw new ProtocolError("RSV1 must not be set on a continuation frame");
                    }
                } else {
                    if (state.messageOpen) {
                        throw new ProtocolError("New data frame while a fragmented message is still open");
                    }
                    state.messageCompressed = frame.rsv1();
                    state.messageOpcode = frame.opcode();
                }
                state.messageOpen = !frame.fin();
                if (state.messageCompressed) {
                    inflated = true;
                    appendPayload(state, input, frame);
                    if (frame.fin()) {
                        output.add(inflateMessage(memoryManager, state));
                    }
                } else {
                    output.add(input.slice(position, frame.end()));
                }
            }
            position = frame.end();
        }

        if (position < limit) {
            // Incomplete frame: copy it out, the underlying buffer does not survive this read
            ByteArrayOutputStream incomplete = new ByteArrayOutputStream();
            incomplete.writeBytes(copyBytes(input, position, limit));
            state.leftover = Optional.of(incomplete);
            state.leftoverRequiredSize = requiredSize;
        } else if (pristine && !inflated) {
            return Inbound.unchanged();
        }

        if (output.isEmpty()) {
            return Inbound.nothing();
        }

        Buffer merged = output.get(0);
        for (int i = 1; i < output.size(); i++) {
            merged = Buffers.appendBuffers(memoryManager, merged, output.get(i));
        }
        return Inbound.of(merged);
    }

    private static void appendPayload(ConnectionState state, Buffer input, Frame frame) {
        if (frame.maskStart() < 0) {
            // RFC 6455 requires a client to mask, and for a compressed frame we are the only parser
            // that sees it, so an unmasked one has to be refused here.
            throw new ProtocolError("Client frames must be masked");
        }
        if (state.messagePayload.isEmpty()) {
            state.messagePayload = Optional.of(new ByteArrayOutputStream());
        }
        if (state.messagePayload.get().size() + frame.payloadLength() > MAX_MESSAGE_SIZE) {
            throw new MessageTooLarge("Compressed message exceeds " + MAX_MESSAGE_SIZE + " bytes");
        }
        byte[] payload = copyBytes(input, frame.payloadStart(), frame.payloadStart() + frame.payloadLength());
        byte[] mask = copyBytes(input, frame.maskStart(), frame.maskStart() + MASK_SIZE);
        for (int i = 0; i < payload.length; i++) {
            payload[i] ^= mask[i % MASK_SIZE];
        }
        state.messagePayload.get().writeBytes(payload);
    }

    private static Buffer inflateMessage(MemoryManager<?> memoryManager, ConnectionState state) {
        byte[] compressed = state.messagePayload.map(ByteArrayOutputStream::toByteArray).orElse(new byte[0]);
        state.messagePayload = Optional.empty();
        return Buffers.wrap(memoryManager, buildFrame(state.messageOpcode, inflate(compressed)));
    }

    /**
     * A truncated payload cannot be told apart from a complete one here, so it yields a short message
     * rather than a protocol error. Every permessage-deflate message ends mid-stream by design — the
     * sender strips the {@link #DEFLATE_TAIL} and never emits a final block — so {@code finished()} is
     * false for valid and truncated input alike, and both end with the inflater having consumed all
     * input and asking for more. Requiring {@code finished()} would reject all legitimate traffic.
     * A peer can therefore only corrupt its own message, which then fails to parse a level up.
     */
    static byte[] inflate(byte[] compressed) {
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(compressed);
            // Sized for the common case and grown on demand: deriving the initial capacity from the
            // compressed size would let a peer dictate a large allocation before a single byte is
            // inflated, which is the very thing MAX_MESSAGE_SIZE is there to prevent.
            ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
            byte[] chunk = new byte[8192];
            boolean tailAdded = false;
            while (true) {
                int count = inflater.inflate(chunk);
                if (count > 0) {
                    if (out.size() + count > MAX_MESSAGE_SIZE) {
                        throw new MessageTooLarge("Inflated message exceeds " + MAX_MESSAGE_SIZE + " bytes");
                    }
                    out.write(chunk, 0, count);
                    continue;
                }
                if (inflater.needsDictionary()) {
                    throw new ProtocolError("Compressed message requires a preset dictionary");
                }
                if (inflater.finished() || !inflater.needsInput() || tailAdded) {
                    break;
                }
                inflater.setInput(DEFLATE_TAIL);
                tailAdded = true;
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            throw new ProtocolError("Could not inflate message: " + e.getMessage());
        } finally {
            inflater.end();
        }
    }

    // Outbound

    static Buffer deflateFrame(MemoryManager<?> memoryManager, Buffer buffer) {
        int position = buffer.position();
        int limit = buffer.limit();
        Frame frame;
        try {
            frame = parseFrame(buffer, position, limit);
        } catch (ProtocolError e) {
            // The inbound validation rules do not apply to what we send: a frame the WebSocketFilter
            // serialized is well-formed by construction and may legitimately exceed the inbound size
            // limit. Anything we cannot parse is simply forwarded uncompressed rather than failing
            // the write, so enabling compression can never make a send fail that would have succeeded.
            log.debug("Sending frame uncompressed, it could not be parsed: {}", e.getMessage());
            return buffer;
        }
        if (!frame.complete()
                || frame.end() != limit
                || frame.isControl()
                || frame.rsv1()
                // A compressed message split over several frames is what a number of client stacks get
                // wrong, so a fragment is sent uncompressed rather than as one fragment of a compressed
                // message. Nothing here fragments today, this keeps it safe if something ever does.
                || !frame.fin()
                // We rebuild the frame unmasked, so a masked one would have to be unmasked first.
                // The server never masks, hence we leave such a frame alone instead.
                || frame.maskStart() >= 0
                || frame.opcode() != OPCODE_TEXT && frame.opcode() != OPCODE_BINARY
                || frame.payloadLength() < MIN_SIZE_TO_COMPRESS) {
            return buffer;
        }

        byte[] payload = copyBytes(buffer, frame.payloadStart(), frame.payloadStart() + frame.payloadLength());
        byte[] compressed = deflate(payload);
        if (compressed.length >= payload.length) {
            return buffer;
        }

        byte[] compressedFrame = buildFrame(frame.opcode(), compressed);
        compressedFrame[0] |= RSV1;
        // Grizzly does not reclaim the buffer that ctx.setMessage() replaces, so a pooled MemoryManager
        // would want a buffer.tryDispose() here. Nothing configures one, so the default HeapMemoryManager
        // applies and disposing would be a no-op. Add the call along with any switch to a pooled manager,
        // and only on this path: every earlier return hands back the very buffer that is about to be
        // written.
        return Buffers.wrap(memoryManager, compressedFrame);
    }

    static byte[] deflate(byte[] payload) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try {
            deflater.setInput(payload);
            ByteArrayOutputStream out = new ByteArrayOutputStream(payload.length);
            byte[] chunk = new byte[8192];
            int count;
            do {
                count = deflater.deflate(chunk, 0, chunk.length, Deflater.SYNC_FLUSH);
                out.write(chunk, 0, count);
            } while (count == chunk.length);

            byte[] compressed = out.toByteArray();
            return endsWithDeflateTail(compressed)
                    ? Arrays.copyOf(compressed, compressed.length - DEFLATE_TAIL.length)
                    : compressed;
        } finally {
            deflater.end();
        }
    }

    private static boolean endsWithDeflateTail(byte[] compressed) {
        int offset = compressed.length - DEFLATE_TAIL.length;
        if (offset < 0) {
            return false;
        }
        for (int i = 0; i < DEFLATE_TAIL.length; i++) {
            if (compressed[offset + i] != DEFLATE_TAIL[i]) {
                return false;
            }
        }
        return true;
    }

    // Framing

    /**
     * @return the parsed frame, or an incomplete frame whose {@code end} tells the caller how far
     * {@code buffer} has to be filled before parsing can be attempted again.
     */
    static Frame parseFrame(Buffer buffer, int start, int limit) {
        if (limit - start < 2) {
            return incomplete(start + 2);
        }
        byte firstByte = buffer.get(start);
        byte secondByte = buffer.get(start + 1);
        if ((firstByte & 0x30) != 0) {
            throw new ProtocolError("RSV2 and RSV3 bits must not be set");
        }
        boolean fin = (firstByte & 0x80) != 0;
        boolean rsv1 = (firstByte & RSV1) != 0;
        byte opcode = (byte) (firstByte & 0x0F);
        boolean masked = (secondByte & 0x80) != 0;
        int lengthCode = secondByte & 0x7F;
        boolean control = (opcode & 0x08) != 0;
        if (control && rsv1) {
            throw new ProtocolError("Control frames must not be compressed");
        }

        int cursor = start + 2;
        long payloadLength;
        if (lengthCode <= 125) {
            payloadLength = lengthCode;
        } else if (lengthCode == 126) {
            if (limit - cursor < 2) {
                return incomplete(cursor + 2);
            }
            payloadLength = (buffer.get(cursor) & 0xFFL) << 8 | buffer.get(cursor + 1) & 0xFFL;
            cursor += 2;
            if (payloadLength <= 125) {
                throw new ProtocolError("Payload length is not minimally encoded");
            }
        } else {
            if (limit - cursor < 8) {
                return incomplete(cursor + 8);
            }
            payloadLength = 0;
            for (int i = 0; i < 8; i++) {
                payloadLength = payloadLength << 8 | buffer.get(cursor + i) & 0xFFL;
            }
            cursor += 8;
            if (payloadLength >= 0 && payloadLength <= 0xFFFF) {
                throw new ProtocolError("Payload length is not minimally encoded");
            }
        }
        if (payloadLength < 0) {
            // RFC 6455: the most significant bit of a 64 bit length must be 0, so this is a malformed
            // frame rather than one that is merely too big, and it gets the close code to match.
            throw new ProtocolError("Payload length must not have the most significant bit set");
        }
        if (payloadLength > MAX_FRAME_PAYLOAD_SIZE) {
            throw new MessageTooLarge("Frame payload exceeds " + MAX_FRAME_PAYLOAD_SIZE + " bytes");
        }

        int maskStart = -1;
        if (masked) {
            if (limit - cursor < MASK_SIZE) {
                return incomplete(cursor + MASK_SIZE);
            }
            maskStart = cursor;
            cursor += MASK_SIZE;
        }
        if (limit - cursor < payloadLength) {
            return incomplete(cursor + (int) payloadLength);
        }
        return new Frame(opcode, fin, rsv1, control, maskStart, cursor, (int) payloadLength,
                cursor + (int) payloadLength, true);
    }

    private static Frame incomplete(int requiredEnd) {
        return new Frame((byte) 0, false, false, false, -1, -1, 0, requiredEnd, false);
    }

    /**
     * Builds an unfragmented, unmasked frame. Inbound this replaces the fragments of an inflated
     * message, outbound it replaces the frame the WebSocketFilter serialized.
     */
    static byte[] buildFrame(byte opcode, byte[] payload) {
        byte[] header;
        int length = payload.length;
        if (length <= 125) {
            header = new byte[]{(byte) (0x80 | opcode), (byte) length};
        } else if (length <= 0xFFFF) {
            header = new byte[]{(byte) (0x80 | opcode), 126, (byte) (length >>> 8), (byte) length};
        } else {
            header = new byte[]{(byte) (0x80 | opcode), 127, 0, 0, 0, 0,
                    (byte) (length >>> 24), (byte) (length >>> 16), (byte) (length >>> 8), (byte) length};
        }
        byte[] frame = new byte[header.length + length];
        System.arraycopy(header, 0, frame, 0, header.length);
        System.arraycopy(payload, 0, frame, header.length, length);
        return frame;
    }

    private static byte[] copyBytes(Buffer buffer, int start, int end) {
        byte[] bytes = new byte[end - start];
        buffer.slice(start, end).get(bytes);
        return bytes;
    }

    /**
     * An inbound size limit was exceeded, which RFC 6455 has a close code of its own for.
     */
    static final class MessageTooLarge extends ProtocolError {
        MessageTooLarge(String message) {
            super(message);
        }
    }

    /**
     * @param content     the frames to forward upwards, empty if the read produced none
     * @param passThrough true if the input needs no rewriting and can be forwarded as it is
     */
    record Inbound(Optional<Buffer> content, boolean passThrough) {
        static Inbound nothing() {
            return new Inbound(Optional.empty(), false);
        }

        static Inbound unchanged() {
            return new Inbound(Optional.empty(), true);
        }

        static Inbound of(Buffer content) {
            return new Inbound(Optional.of(content), false);
        }
    }

    /**
     * @param end      for a complete frame the offset just past it, otherwise the offset the buffer
     *                 has to reach before the frame can be parsed
     * @param complete false if the buffer does not hold the frame yet, in which case only
     *                 {@code end} carries a meaning
     */
    record Frame(byte opcode,
                 boolean fin,
                 boolean rsv1,
                 boolean isControl,
                 int maskStart,
                 int payloadStart,
                 int payloadLength,
                 int end,
                 boolean complete) {
    }

    /**
     * Reads for one connection are serialized but not pinned to a single worker thread, and
     * {@code enabled} is additionally set from the write path, so the fields are volatile for the same
     * reason Grizzly's own {@code WebSocketHolder} makes its state volatile.
     *
     * <p>Note what carries the safety here: {@code volatile} publishes the reference, not the later
     * content of the {@link ByteArrayOutputStream} each one points at, and those buffers are mutated in
     * place. Correctness rests solely on Grizzly serializing the reads of one connection, which puts a
     * happens-before edge between consecutive read events. A refactor that moves this work off that
     * thread has to add real synchronization; keeping the fields volatile would not be enough.
     *
     * <p>The outbound path needs no state at all: {@code server_no_context_takeover} means every message
     * is deflated with its own {@link Deflater}, so concurrent sends cannot interfere.
     */
    static final class ConnectionState {
        private volatile boolean enabled;
        private volatile Optional<ByteArrayOutputStream> leftover = Optional.empty();

        private boolean isEnabled() {
            return enabled;
        }

        private volatile int leftoverRequiredSize;
        private volatile boolean messageOpen;
        private volatile boolean messageCompressed;
        private volatile byte messageOpcode;
        private volatile Optional<ByteArrayOutputStream> messagePayload = Optional.empty();
    }
}
