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

import bisq.api.web_socket.compression.PerMessageDeflateFilter.ConnectionState;
import bisq.api.web_socket.compression.PerMessageDeflateFilter.Frame;
import bisq.api.web_socket.compression.PerMessageDeflateFilter.Inbound;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.websockets.ProtocolError;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerMessageDeflateFilterTest {
    private static final byte OPCODE_CONTINUATION = 0x0;
    private static final byte OPCODE_TEXT = 0x1;
    private static final byte OPCODE_PING = 0x9;
    private static final byte RSV1 = 0x40;
    private static final byte[] MASK = {0x11, 0x22, 0x33, 0x44};

    private final MemoryManager<?> memoryManager = new HeapMemoryManager();
    private final ConnectionState state = new ConnectionState();

    // Negotiation

    @Test
    void acceptsBareOffer() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate")).isTrue();
    }

    @Test
    void acceptsOfferWithNoContextTakeoverParameters() {
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; client_no_context_takeover; server_no_context_takeover")).isTrue();
    }

    /**
     * The client's window bounds the compressor at the other end, and RFC 7692 lets the server ignore
     * the value: our Inflater always uses the largest window and decodes a stream produced with any
     * smaller one, so such an offer must not cost the connection its compression.
     */
    @Test
    void acceptsAnyClientWindowSizeAndIgnoresIt() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits")).isTrue();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=8")).isTrue();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=10")).isTrue();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=15")).isTrue();
    }

    /**
     * The server's window bounds our own compressor, which java.util.zip cannot restrict, so anything
     * but the largest has to be declined. An offer has to carry a value for it, so one without is
     * declined as well.
     */
    @Test
    void declinesAServerWindowSizeItCannotHonour() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; server_max_window_bits=15")).isTrue();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; server_max_window_bits=10")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; server_max_window_bits")).isFalse();
    }

    @Test
    void declinesAWindowSizeOutsideTheRange() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=7")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=16")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=08")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; client_max_window_bits=x")).isFalse();
    }

    /**
     * RFC 7692 defines both takeover parameters as flags, so an offer giving one a value is malformed.
     */
    @Test
    void declinesANoContextTakeoverParameterCarryingAValue() {
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; client_no_context_takeover=1")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; server_no_context_takeover=true")).isFalse();
    }

    @Test
    void declinesAnOfferRepeatingAParameter() {
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; client_no_context_takeover; client_no_context_takeover")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; server_max_window_bits=15; server_max_window_bits=15")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; Client_No_Context_Takeover; client_no_context_takeover")).isFalse();
    }

    /**
     * A repetition only disqualifies the offer it appears in: a client is free to list the same
     * parameter once in each of several alternative offers.
     */
    @Test
    void countsRepetitionsPerOfferRatherThanPerHeader() {
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; client_no_context_takeover, permessage-deflate; client_no_context_takeover"))
                .isTrue();
    }

    @Test
    void declinesUnknownParameterAndUnknownExtension() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; unexpected=1")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("x-webkit-deflate-frame")).isFalse();
    }

    @Test
    void acceptsAQuotedParameterValue() {
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; server_max_window_bits=\"15\"")).isTrue();
    }

    /**
     * RFC 6455 lets a parameter value be a quoted string, so a separator inside one is data. Reading it
     * as a separator would let the value of an unrelated extension pose as an offer of our own.
     */
    @Test
    void treatsSeparatorsInsideAQuotedValueAsData() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("x-foo; bar=\"a, permessage-deflate ,b\"")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "x-foo; bar=\"a\\\", permessage-deflate ,b\"")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; server_max_window_bits=\"15; client_max_window_bits\"")).isFalse();
    }

    /**
     * RFC 6455 has a ";" introduce a parameter, so an offer ending in one, or holding an empty part
     * between two of them, is malformed rather than an offer with a parameter left out.
     */
    @Test
    void declinesAnOfferWithAnEmptyParameter() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate;")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate;; client_no_context_takeover")).isFalse();
    }

    @Test
    void declinesAHeaderWithAnUnclosedQuote() {
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate; server_max_window_bits=\"15")).isFalse();
        assertThat(PerMessageDeflateFilter.acceptsOffer("permessage-deflate, x-foo; bar=\"a")).isFalse();
    }

    @Test
    void picksTheAcceptableOfferFromAList() {
        assertThat(PerMessageDeflateFilter.acceptsOffer(
                "permessage-deflate; server_max_window_bits=10, permessage-deflate")).isTrue();
    }

    // Codec

    @Test
    void deflateAndInflateRoundTrip() {
        byte[] payload = repeat("{\"topic\":\"OFFERS\",\"payload\":\"abcdefghij\"}", 20);

        byte[] compressed = PerMessageDeflateFilter.deflate(payload);

        assertThat(compressed.length).isLessThan(payload.length);
        assertThat(PerMessageDeflateFilter.inflate(compressed)).isEqualTo(payload);
    }

    @Test
    void inflateRejectsGarbage() {
        assertThatThrownBy(() -> PerMessageDeflateFilter.inflate(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}))
                .isInstanceOf(ProtocolError.class);
    }

    // Outbound

    @Test
    void outboundCompressesLargeTextFrame() {
        byte[] payload = repeat("bisq", 100);
        Buffer plain = Buffers.wrap(memoryManager, PerMessageDeflateFilter.buildFrame(OPCODE_TEXT, payload));

        Buffer compressed = PerMessageDeflateFilter.deflateFrame(memoryManager, plain);

        Frame frame = PerMessageDeflateFilter.parseFrame(compressed, compressed.position(), compressed.limit());
        assertThat(frame.complete()).isTrue();
        assertThat(frame.rsv1()).isTrue();
        assertThat(frame.fin()).isTrue();
        assertThat(frame.opcode()).isEqualTo(OPCODE_TEXT);
        assertThat(frame.payloadLength()).isLessThan(payload.length);
        assertThat(PerMessageDeflateFilter.inflate(payloadOf(compressed, frame))).isEqualTo(payload);
    }

    @Test
    void outboundLeavesSmallAndIncompressibleFramesUntouched() {
        Buffer small = Buffers.wrap(memoryManager,
                PerMessageDeflateFilter.buildFrame(OPCODE_TEXT, "ping".getBytes(StandardCharsets.UTF_8)));
        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, small)).isSameAs(small);

        // Already compressed data does not shrink any further, so it must be sent as it is
        byte[] incompressible = PerMessageDeflateFilter.deflate(repeat("bisq", 100));
        Buffer frame = Buffers.wrap(memoryManager, PerMessageDeflateFilter.buildFrame(OPCODE_TEXT, incompressible));
        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, frame)).isSameAs(frame);
    }

    @Test
    void outboundLeavesMaskedFrameUntouched() {
        Buffer masked = Buffers.wrap(memoryManager, maskedFrame(OPCODE_TEXT, false, true, repeat("bisq", 100)));
        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, masked)).isSameAs(masked);
    }

    @Test
    void outboundLeavesFrameItCannotParseUntouched() {
        // The inbound size limit must not make a send fail: such a frame is forwarded as it is
        byte[] header = {(byte) (0x80 | OPCODE_TEXT), 127, 0, 0, 0, 0, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        Buffer oversized = Buffers.wrap(memoryManager, header);

        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, oversized)).isSameAs(oversized);
    }

    @Test
    void outboundLeavesFragmentsUncompressed() {
        // Clients that mishandle a compressed message split over several frames must not be exposed to
        // one, so neither the first fragment nor a continuation may be compressed
        byte[] payload = repeat("bisq", 100);
        byte[] firstFragment = PerMessageDeflateFilter.buildFrame(OPCODE_TEXT, payload);
        firstFragment[0] &= 0x7F;
        Buffer notFinal = Buffers.wrap(memoryManager, firstFragment);
        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, notFinal)).isSameAs(notFinal);

        Buffer continuation = Buffers.wrap(memoryManager,
                PerMessageDeflateFilter.buildFrame(OPCODE_CONTINUATION, payload));
        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, continuation)).isSameAs(continuation);
    }

    @Test
    void outboundLeavesControlFramesUntouched() {
        Buffer ping = Buffers.wrap(memoryManager, PerMessageDeflateFilter.buildFrame(OPCODE_PING, repeat("x", 100)));
        assertThat(PerMessageDeflateFilter.deflateFrame(memoryManager, ping)).isSameAs(ping);
    }

    // Inbound

    @Test
    void inboundForwardsUncompressedFramesUnchanged() {
        Buffer input = Buffers.wrap(memoryManager, maskedFrame(OPCODE_TEXT, false, true, "hello".getBytes(StandardCharsets.UTF_8)));

        Inbound inbound = PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state);

        assertThat(inbound.passThrough()).isTrue();
        assertThat(inbound.content()).isEmpty();
    }

    @Test
    void inboundInflatesCompressedMessage() {
        byte[] payload = repeat("subscribe", 30);
        Buffer input = Buffers.wrap(memoryManager,
                maskedFrame(OPCODE_TEXT, true, true, PerMessageDeflateFilter.deflate(payload)));

        Inbound inbound = PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state);

        assertThat(inbound.passThrough()).isFalse();
        assertPlainTextFrame(inbound.content(), payload);
    }

    @Test
    void inboundReassemblesCompressedFragments() {
        byte[] payload = repeat("fragment", 30);
        byte[] compressed = PerMessageDeflateFilter.deflate(payload);
        int split = compressed.length / 2;
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.writeBytes(maskedFrame(OPCODE_TEXT, true, false, slice(compressed, 0, split)));
        input.writeBytes(maskedFrame(OPCODE_CONTINUATION, false, true, slice(compressed, split, compressed.length)));

        Inbound inbound = PerMessageDeflateFilter.processInbound(memoryManager,
                Optional.of(Buffers.wrap(memoryManager, input.toByteArray())), state);

        assertPlainTextFrame(inbound.content(), payload);
    }

    @Test
    void inboundBuffersPartialFrameUntilItIsComplete() {
        byte[] payload = repeat("partial", 30);
        byte[] frame = maskedFrame(OPCODE_TEXT, true, true, PerMessageDeflateFilter.deflate(payload));
        int split = frame.length / 2;

        Inbound first = PerMessageDeflateFilter.processInbound(memoryManager,
                Optional.of(Buffers.wrap(memoryManager, slice(frame, 0, split))), state);
        assertThat(first.passThrough()).isFalse();
        assertThat(first.content()).isEmpty();

        Inbound second = PerMessageDeflateFilter.processInbound(memoryManager,
                Optional.of(Buffers.wrap(memoryManager, slice(frame, split, frame.length))), state);
        assertPlainTextFrame(second.content(), payload);
    }

    @Test
    void inboundPassesControlFrameInterleavedWithFragmentsThrough() {
        byte[] payload = repeat("interleaved", 30);
        byte[] compressed = PerMessageDeflateFilter.deflate(payload);
        int split = compressed.length / 2;
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.writeBytes(maskedFrame(OPCODE_TEXT, true, false, slice(compressed, 0, split)));
        input.writeBytes(maskedFrame(OPCODE_PING, false, true, new byte[0]));
        input.writeBytes(maskedFrame(OPCODE_CONTINUATION, false, true, slice(compressed, split, compressed.length)));

        Inbound inbound = PerMessageDeflateFilter.processInbound(memoryManager,
                Optional.of(Buffers.wrap(memoryManager, input.toByteArray())), state);

        Buffer content = inbound.content().orElseThrow();
        Frame ping = PerMessageDeflateFilter.parseFrame(content, content.position(), content.limit());
        assertThat(ping.complete()).isTrue();
        assertThat(ping.opcode()).isEqualTo(OPCODE_PING);

        Frame text = PerMessageDeflateFilter.parseFrame(content, ping.end(), content.limit());
        assertThat(text.complete()).isTrue();
        assertThat(text.opcode()).isEqualTo(OPCODE_TEXT);
        assertThat(text.rsv1()).isFalse();
        assertThat(payloadOf(content, text)).isEqualTo(payload);
    }

    @Test
    void inboundRejectsCompressedControlFrame() {
        Buffer input = Buffers.wrap(memoryManager, maskedFrame(OPCODE_PING, true, true, new byte[0]));

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state))
                .isInstanceOf(ProtocolError.class);
    }

    @Test
    void inboundReassemblesFrameDribbledByteByByte() {
        byte[] payload = repeat("dribble", 30);
        byte[] frame = maskedFrame(OPCODE_TEXT, true, true, PerMessageDeflateFilter.deflate(payload));

        Inbound inbound = Inbound.nothing();
        for (int i = 0; i < frame.length; i++) {
            inbound = PerMessageDeflateFilter.processInbound(memoryManager,
                    Optional.of(Buffers.wrap(memoryManager, slice(frame, i, i + 1))), state);
            if (i < frame.length - 1) {
                assertThat(inbound.content()).isEmpty();
            }
        }
        assertPlainTextFrame(inbound.content(), payload);
    }

    @Test
    void inboundRejectsNonMinimalLengthEncoding() {
        byte[] header = {(byte) (0x80 | OPCODE_TEXT), 126, 0, 10};
        Buffer input = Buffers.wrap(memoryManager, header);

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state))
                .isInstanceOf(ProtocolError.class);
    }

    @Test
    void inboundRejectsOversizedFrame() {
        byte[] header = {(byte) (0x80 | OPCODE_TEXT), (byte) (0x80 | 127),
                0, 0, 0, 0, 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        Buffer input = Buffers.wrap(memoryManager, header);

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state))
                .isInstanceOf(ProtocolError.class);
    }

    /**
     * The per-frame limit does not bound a message split over several frames, so the accumulated size
     * is what has to stop a peer from growing the buffer without end.
     */
    @Test
    void inboundRejectsAMessageThatGrowsBeyondTheLimitAcrossFragments() {
        int fragmentSize = 6 * 1024 * 1024;
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.writeBytes(maskedFrame(OPCODE_TEXT, true, false, new byte[fragmentSize]));
        input.writeBytes(maskedFrame(OPCODE_CONTINUATION, false, false, new byte[fragmentSize]));

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager,
                Optional.of(Buffers.wrap(memoryManager, input.toByteArray())), state))
                .isInstanceOf(PerMessageDeflateFilter.MessageTooLarge.class);
    }

    /**
     * The zip bomb case the inflated-size limit exists for: a payload small enough to pass every other
     * check that expands far beyond what we are willing to hold.
     */
    @Test
    void inflateRejectsAPayloadThatExpandsBeyondTheLimit() {
        byte[] bomb = PerMessageDeflateFilter.deflate(new byte[11 * 1024 * 1024]);
        assertThat(bomb.length).isLessThan(64 * 1024);

        assertThatThrownBy(() -> PerMessageDeflateFilter.inflate(bomb))
                .isInstanceOf(PerMessageDeflateFilter.MessageTooLarge.class);
    }

    /**
     * The frames of a compressed message are consumed here rather than forwarded, so the WebSocketFilter
     * above never sees them and the fragmentation rules it would enforce have to be enforced here.
     */
    @Test
    void inboundRejectsAContinuationWithoutAMessageToContinue() {
        Buffer input = Buffers.wrap(memoryManager,
                maskedFrame(OPCODE_CONTINUATION, false, true, "orphan".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state))
                .isInstanceOf(ProtocolError.class);
    }

    @Test
    void inboundRejectsANewDataFrameWhileAMessageIsStillOpen() {
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.writeBytes(maskedFrame(OPCODE_TEXT, true, false, PerMessageDeflateFilter.deflate(repeat("open", 20))));
        input.writeBytes(maskedFrame(OPCODE_TEXT, false, true, "interrupting".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager,
                Optional.of(Buffers.wrap(memoryManager, input.toByteArray())), state))
                .isInstanceOf(ProtocolError.class);
    }

    /**
     * RFC 6455 requires a client to mask. Unmasked, a compressed frame would be rebuilt as the unmasked
     * frame the WebSocketFilter expects from us, so nothing further up could tell the two apart.
     */
    @Test
    void inboundRejectsAnUnmaskedCompressedFrame() {
        byte[] compressed = PerMessageDeflateFilter.deflate(repeat("unmasked", 20));
        byte[] frame = PerMessageDeflateFilter.buildFrame(OPCODE_TEXT, compressed);
        frame[0] |= RSV1;
        Buffer input = Buffers.wrap(memoryManager, frame);

        assertThatThrownBy(() -> PerMessageDeflateFilter.processInbound(memoryManager, Optional.of(input), state))
                .isInstanceOf(ProtocolError.class);
    }

    // Helpers

    private void assertPlainTextFrame(Optional<Buffer> maybeContent, byte[] expectedPayload) {
        assertThat(maybeContent).isPresent();
        Buffer content = maybeContent.get();
        Frame frame = PerMessageDeflateFilter.parseFrame(content, content.position(), content.limit());
        assertThat(frame.complete()).isTrue();
        assertThat(frame.opcode()).isEqualTo(OPCODE_TEXT);
        assertThat(frame.fin()).isTrue();
        assertThat(frame.rsv1()).isFalse();
        assertThat(payloadOf(content, frame)).isEqualTo(expectedPayload);
    }

    private static byte[] payloadOf(Buffer buffer, Frame frame) {
        byte[] payload = new byte[frame.payloadLength()];
        buffer.slice(frame.payloadStart(), frame.payloadStart() + frame.payloadLength()).get(payload);
        if (frame.maskStart() >= 0) {
            byte[] mask = new byte[4];
            buffer.slice(frame.maskStart(), frame.maskStart() + 4).get(mask);
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i % 4];
            }
        }
        return payload;
    }

    /**
     * Builds a frame as a client sends it: masked, and with RSV1 marking a compressed message.
     */
    private static byte[] maskedFrame(byte opcode, boolean rsv1, boolean fin, byte[] payload) {
        byte[] plain = PerMessageDeflateFilter.buildFrame(opcode, payload);
        int headerLength = plain.length - payload.length;
        byte[] frame = new byte[plain.length + MASK.length];
        System.arraycopy(plain, 0, frame, 0, headerLength);
        if (!fin) {
            frame[0] &= 0x7F;
        }
        if (rsv1) {
            frame[0] |= RSV1;
        }
        frame[1] |= (byte) 0x80;
        System.arraycopy(MASK, 0, frame, headerLength, MASK.length);
        for (int i = 0; i < payload.length; i++) {
            frame[headerLength + MASK.length + i] = (byte) (payload[i] ^ MASK[i % MASK.length]);
        }
        return frame;
    }

    private static byte[] slice(byte[] bytes, int from, int to) {
        byte[] result = new byte[to - from];
        System.arraycopy(bytes, from, result, 0, result.length);
        return result;
    }

    private static byte[] repeat(String value, int times) {
        return value.repeat(times).getBytes(StandardCharsets.UTF_8);
    }
}
