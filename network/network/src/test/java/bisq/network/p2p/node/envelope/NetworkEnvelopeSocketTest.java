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

package bisq.network.p2p.node.envelope;

import bisq.common.network.PeerSocket;
import bisq.common.util.ByteArrayUtils;
import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.WireFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static bisq.network.p2p.message.NetworkEnvelope.MAX_SERIALIZED_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetworkEnvelopeSocketTest {
    private static final byte[] NEXT_BYTES = {1, 2, 3};

    @Test
    void sendsAndReceivesConsecutiveMessages() throws IOException {
        NetworkEnvelope first = createEnvelope(new Ping(1));
        NetworkEnvelope second = createEnvelope(new Ping(2));
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        NetworkEnvelopeSocket sender = createSender(sent);
        sender.send(first);
        sender.send(second);

        NetworkEnvelopeSocket receiver = createReceiver(sent.toByteArray());

        assertThat(receiver.receiveNextEnvelope()).isEqualTo(first.completeProto());
        assertThat(receiver.receiveNextEnvelope()).isEqualTo(second.completeProto());
        assertThat(receiver.receiveNextEnvelope()).isNull();
    }

    @Test
    void sendsAndReceivesAMessageOfTheSizeLimit() throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = createProtoOfSize(MAX_SERIALIZED_SIZE);
        NetworkEnvelope networkEnvelope = mock(NetworkEnvelope.class);
        when(networkEnvelope.completeProto()).thenReturn(proto);
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        createSender(sent).send(networkEnvelope);

        NetworkEnvelopeSocket receiver = createReceiver(sent.toByteArray());

        assertThat(sent.toByteArray()).isEqualTo(writeDelimited(proto));
        assertThat(receiver.receiveNextEnvelope()).isEqualTo(proto);
    }

    @Test
    void appliesTheSizeLimitToEachMessage() throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = createProtoOfSize(MAX_SERIALIZED_SIZE / 2 + 1);
        byte[] message = writeDelimited(proto);
        NetworkEnvelopeSocket receiver = createReceiver(ByteArrayUtils.concat(message, message));

        assertThat(receiver.receiveNextEnvelope()).isEqualTo(proto);
        assertThat(receiver.receiveNextEnvelope()).isEqualTo(proto);
        assertThat(receiver.receiveNextEnvelope()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4096})
    void doesNotReadBytesOfTheNextMessage(int maxBytesPerRead) throws IOException {
        // The handshake and then the connection read from the same input stream, each with its own socket. Bytes of
        // the next message which the socket of the handshake read would be lost.
        bisq.network.protobuf.NetworkEnvelope first = createProto(1);
        bisq.network.protobuf.NetworkEnvelope second = createProto(2);
        byte[] secondMessage = writeDelimited(second);
        byte[] receivedBytes = ByteArrayUtils.concat(writeDelimited(first), secondMessage);
        ByteArrayInputStream input = new ByteArrayInputStream(receivedBytes) {
            @Override
            public synchronized int read(byte[] bytes, int offset, int length) {
                return super.read(bytes, offset, Math.min(length, maxBytesPerRead));
            }
        };

        assertThat(createReceiver(input).receiveNextEnvelope()).isEqualTo(first);
        assertThat(input.available()).isEqualTo(secondMessage.length);
        NetworkEnvelopeSocket socketOfTheConnection = createReceiver(input);
        assertThat(socketOfTheConnection.receiveNextEnvelope()).isEqualTo(second);
        assertThat(socketOfTheConnection.receiveNextEnvelope()).isNull();
    }

    @Test
    void readsAMessageOfLengthZero() throws IOException {
        // A length prefix of 0 announces a message without fields. It is not the end of the stream.
        NetworkEnvelopeSocket receiver = createReceiver(new byte[]{0});

        assertThat(receiver.receiveNextEnvelope())
                .isEqualTo(bisq.network.protobuf.NetworkEnvelope.getDefaultInstance());
        assertThat(receiver.receiveNextEnvelope()).isNull();
    }

    @Test
    void doesNotSendAMessageAboveTheSizeLimit() {
        bisq.network.protobuf.NetworkEnvelope proto = createProtoOfSize(MAX_SERIALIZED_SIZE + 1).toBuilder()
                .setNetworkMessage(bisq.network.protobuf.EnvelopePayloadMessage.newBuilder()
                        .setPing(bisq.network.protobuf.Ping.getDefaultInstance()))
                .build();
        NetworkEnvelope networkEnvelope = mock(NetworkEnvelope.class);
        when(networkEnvelope.completeProto()).thenReturn(proto);
        ByteArrayOutputStream sent = new ByteArrayOutputStream();
        NetworkEnvelopeSocket sender = createSender(sent);

        assertThatThrownBy(() -> sender.send(networkEnvelope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PING");
        assertThat(sent.size()).isZero();
    }

    @Test
    void rejectsALengthPrefixAboveTheSizeLimitBeforeReadingTheMessage() throws IOException {
        byte[] receivedBytes = ByteArrayUtils.concat(lengthPrefix(MAX_SERIALIZED_SIZE + 1), NEXT_BYTES);
        ByteArrayInputStream input = new ByteArrayInputStream(receivedBytes);
        NetworkEnvelopeSocket receiver = createReceiver(input);

        assertThatThrownBy(receiver::receiveNextEnvelope)
                .isInstanceOf(InvalidProtocolBufferException.class);
        // Only the length prefix was read.
        assertThat(input.readAllBytes()).isEqualTo(NEXT_BYTES);
    }

    @ParameterizedTest
    @ValueSource(longs = {MAX_SERIALIZED_SIZE + 1, Integer.MAX_VALUE, 1L << 31, (1L << 32) - 1, 1L << 32,
            (1L << 32) + 5, (1L << 35) - 1})
    void rejectsALengthAboveTheSizeLimitWithoutTruncatingIt(long length) {
        // A length of 2^32 + 5 has 5 bytes. Truncated to 32 bits, it would be 5.
        NetworkEnvelopeSocket receiver = createReceiver(inputWhichFailsAfter(lengthPrefix(length)));

        assertThatThrownBy(receiver::receiveNextEnvelope)
                .isInstanceOf(InvalidProtocolBufferException.class);
    }

    @Test
    void rejectsALengthPrefixOfMoreThanFiveBytes() throws IOException {
        byte[][] lengthPrefixes = {
                // Value 0 in 6 bytes. Protobuf writes a length with at most 5 bytes.
                {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00},
                // Value 2^64 + 5 in 10 bytes. readRawVarint64 decodes it as 5.
                {(byte) 0x85, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                        (byte) 0x80, (byte) 0x80, 0x02},
                // 11 bytes: not a varint.
                {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                        (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00}
        };
        for (byte[] lengthPrefix : lengthPrefixes) {
            ByteArrayInputStream input = new ByteArrayInputStream(ByteArrayUtils.concat(lengthPrefix, NEXT_BYTES));
            NetworkEnvelopeSocket receiver = createReceiver(input);

            assertThatThrownBy(receiver::receiveNextEnvelope)
                    .isInstanceOf(InvalidProtocolBufferException.class);
            // The socket did not read the message.
            assertThat(input.available()).isGreaterThanOrEqualTo(NEXT_BYTES.length);
        }
    }

    @Test
    void failsIfTheStreamEndsInsideTheLengthPrefix() {
        NetworkEnvelopeSocket receiver = createReceiver(new byte[]{(byte) 0x80});

        assertThatThrownBy(receiver::receiveNextEnvelope)
                .isInstanceOf(InvalidProtocolBufferException.class);
    }

    @Test
    void failsIfTheStreamEndsInsideAMessage() {
        // The received bytes are a valid envelope, but the length prefix announces one byte more.
        byte[] message = createProto(1).toByteArray();
        NetworkEnvelopeSocket receiver = createReceiver(
                ByteArrayUtils.concat(lengthPrefix(message.length + 1), message));

        assertThatThrownBy(receiver::receiveNextEnvelope)
                .isInstanceOf(EOFException.class);
    }

    @Test
    void rejectsATruncatedFieldWithoutReadingTheNextMessage() throws IOException {
        // A message of one byte: the tag of the field version, without its value.
        byte[] nextMessage = writeDelimited(createProto(1));
        ByteArrayInputStream input = new ByteArrayInputStream(ByteArrayUtils.concat(new byte[]{1, 0x08}, nextMessage));
        NetworkEnvelopeSocket receiver = createReceiver(input);

        assertThatThrownBy(receiver::receiveNextEnvelope)
                .isInstanceOf(InvalidProtocolBufferException.class);
        assertThat(input.readAllBytes()).isEqualTo(nextMessage);
    }

    @Test
    void rejectsAFieldWhichIsLongerThanItsMessage() throws IOException {
        // A small message with a field which announces 2^31 - 1 bytes. The parser must not read or allocate them.
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(body);
        codedOutputStream.writeTag(1000, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        codedOutputStream.writeUInt32NoTag(Integer.MAX_VALUE);
        codedOutputStream.flush();
        NetworkEnvelopeSocket receiver = createReceiver(
                ByteArrayUtils.concat(lengthPrefix(body.size()), body.toByteArray()));

        assertThatThrownBy(receiver::receiveNextEnvelope)
                .isInstanceOf(InvalidProtocolBufferException.class);
    }

    @Test
    void rejectsAnEndGroupTagWithoutAStartGroupTag() {
        // 0x08 0x02 is the field version with value 1. 0x0C is the end-group tag of field 1, once at the end and once
        // at the start of the message.
        byte[][] messages = {{0x08, 0x02, 0x0C}, {0x0C, 0x08, 0x02}};
        for (byte[] message : messages) {
            NetworkEnvelopeSocket receiver = createReceiver(
                    ByteArrayUtils.concat(lengthPrefix(message.length), message));

            assertThatThrownBy(receiver::receiveNextEnvelope)
                    .isInstanceOf(InvalidProtocolBufferException.class);
        }
    }

    // A peer which sends a byte shortly before each socket timeout must not keep the connection without limit (rule 10
    // of docs/specifications/network/message-framing.md). Each read here returns 1 byte and takes 61 seconds.
    @Test
    void failsIfTheBytesOfAMessageDoNotArriveWithinTheDeadline() throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = createProtoOfSize(10);
        AtomicLong nanoTime = new AtomicLong();
        NetworkEnvelopeSocket receiver = createReceiver(slowInput(writeDelimited(proto), nanoTime, 61), nanoTime);

        assertThatThrownBy(receiver::receiveNextMessage)
                .isInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void failsIfTheFinalBytesArriveAfterTheDeadline() throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = createProtoOfSize(10);
        AtomicLong nanoTime = new AtomicLong();
        // The first nine body bytes arrive in 225 seconds; only the final read crosses the deadline.
        NetworkEnvelopeSocket receiver = createReceiver(slowInput(writeDelimited(proto), nanoTime, 25), nanoTime);

        assertThatThrownBy(receiver::receiveNextMessage)
                .isInstanceOf(SocketTimeoutException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 24})
    void readsAMessageWhoseBytesArriveWithinTheDeadline(int secondsPerRead) throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = createProtoOfSize(10);
        AtomicLong nanoTime = new AtomicLong();
        // Ten body reads, completing before or exactly at the deadline.
        NetworkEnvelopeSocket receiver = createReceiver(slowInput(writeDelimited(proto), nanoTime, secondsPerRead), nanoTime);

        assertThat(receiver.receiveNextEnvelope()).isEqualTo(proto);
    }

    @Test
    void returnsNullIfTheStreamEndsBeforeTheNextMessage() throws IOException {
        assertThat(createReceiver(new byte[0]).receiveNextEnvelope()).isNull();
    }

    private static NetworkEnvelope createEnvelope(Ping ping) {
        AuthorizationService authorizationService = new AuthorizationService(
                new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH_V2)),
                new HashCashProofOfWorkService(),
                new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH_V2));
        AuthorizationToken token = authorizationService.createToken(ping,
                new NetworkLoad(),
                "127.0.0.1:1234",
                1,
                List.of(Feature.AUTHORIZATION_HASH_CASH_V2));
        return new NetworkEnvelope(token, ping);
    }

    // An envelope with an unknown field which pads it to the given serialized size.
    private static bisq.network.protobuf.NetworkEnvelope createProtoOfSize(int serializedSize) {
        int paddingFieldNumber = 1000;
        int versionFieldSize = 2;
        int fieldSize = serializedSize - versionFieldSize - CodedOutputStream.computeTagSize(paddingFieldNumber);
        // The length of the padding precedes the padding. It can need one byte fewer than the length of the field.
        int paddingSize = fieldSize - CodedOutputStream.computeUInt32SizeNoTag(fieldSize);
        paddingSize = fieldSize - CodedOutputStream.computeUInt32SizeNoTag(paddingSize);
        UnknownFieldSet padding = UnknownFieldSet.newBuilder()
                .addField(paddingFieldNumber, UnknownFieldSet.Field.newBuilder()
                        .addLengthDelimited(ByteString.copyFrom(new byte[paddingSize]))
                        .build())
                .build();
        bisq.network.protobuf.NetworkEnvelope proto = bisq.network.protobuf.NetworkEnvelope.newBuilder()
                .setVersion(1)
                .setUnknownFields(padding)
                .build();
        assertThat(proto.getSerializedSize()).isEqualTo(serializedSize);
        return proto;
    }

    private static byte[] lengthPrefix(long length) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
            codedOutputStream.writeUInt64NoTag(length);
            codedOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    // Returns the given bytes. A read of a further byte fails the test.
    private static InputStream inputWhichFailsAfter(byte[] bytes) {
        return new InputStream() {
            private int position;

            @Override
            public int read() {
                if (position == bytes.length) {
                    throw new AssertionError("The socket read more than " + bytes.length + " bytes");
                }
                return Byte.toUnsignedInt(bytes[position++]);
            }
        };
    }

    private static bisq.network.protobuf.NetworkEnvelope createProto(int version) {
        return bisq.network.protobuf.NetworkEnvelope.newBuilder().setVersion(version).build();
    }

    private static byte[] writeDelimited(bisq.network.protobuf.NetworkEnvelope proto) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        proto.writeDelimitedTo(outputStream);
        return outputStream.toByteArray();
    }

    private static NetworkEnvelopeSocket createSender(OutputStream outputStream) {
        return new NetworkEnvelopeSocket(new StreamPeerSocket(InputStream.nullInputStream(), outputStream),
                MAX_SERIALIZED_SIZE);
    }

    private static NetworkEnvelopeSocket createReceiver(byte[] receivedBytes) {
        return createReceiver(new ByteArrayInputStream(receivedBytes));
    }

    private static NetworkEnvelopeSocket createReceiver(InputStream inputStream) {
        return new NetworkEnvelopeSocket(new StreamPeerSocket(inputStream, OutputStream.nullOutputStream()),
                MAX_SERIALIZED_SIZE);
    }

    // Returns at most 1 byte per read and advances the time by the given seconds with each read
    private static InputStream slowInput(byte[] bytes, AtomicLong nanoTime, long secondsPerRead) {
        return new ByteArrayInputStream(bytes) {
            @Override
            public synchronized int read(byte[] buffer, int offset, int length) {
                nanoTime.addAndGet(TimeUnit.SECONDS.toNanos(secondsPerRead));
                return super.read(buffer, offset, Math.min(length, 1));
            }
        };
    }

    private static NetworkEnvelopeSocket createReceiver(InputStream inputStream, AtomicLong nanoTime) {
        return new NetworkEnvelopeSocket(new StreamPeerSocket(inputStream, OutputStream.nullOutputStream()),
                MAX_SERIALIZED_SIZE,
                nanoTime::get);
    }

    private record StreamPeerSocket(InputStream inputStream, OutputStream outputStream) implements PeerSocket {
        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() {
        }
    }
}
