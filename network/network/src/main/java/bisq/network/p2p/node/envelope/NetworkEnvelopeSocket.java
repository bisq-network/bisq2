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
import bisq.network.p2p.message.NetworkEnvelope;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Sends and receives messages with the framing of docs/specifications/network/message-framing.md: a varint length
 * prefix, followed by the serialized envelope. The owner of the socket passes the size limit of the envelope.
 */
@Slf4j
public class NetworkEnvelopeSocket implements Closeable {
    // Protobuf writes the length as a varint of 32 bits, which has at most 5 bytes.
    private static final int MAX_LENGTH_PREFIX_SIZE = 5;

    private final PeerSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final int maxSerializedSize;

    public NetworkEnvelopeSocket(PeerSocket socket, int maxSerializedSize) {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.maxSerializedSize = maxSerializedSize;
    }

    public void send(NetworkEnvelope networkEnvelope) throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = networkEnvelope.completeProto();
        int serializedSize = proto.getSerializedSize();
        // Only a defect or the configuration of this node can create a larger message, for example a high inventory
        // size limit. The type of the message helps to find the cause.
        checkArgument(serializedSize <= maxSerializedSize,
                "The %s message has %s bytes. The size limit is %s bytes.",
                proto.getNetworkMessage().getMessageCase(), serializedSize, maxSerializedSize);
        proto.writeDelimitedTo(outputStream);
        outputStream.flush();
    }

    /**
     * Reads and parses the next message.
     *
     * @return The next message, or null if the input stream ends before the first byte of a length prefix.
     */
    @Nullable
    public bisq.network.protobuf.NetworkEnvelope receiveNextEnvelope() throws IOException {
        byte[] message = receiveNextMessage();
        return message == null ? null : parse(message);
    }

    /**
     * Reads the bytes of the next message (see docs/specifications/network/message-framing.md). The method runs before
     * any other check of the peer, also before the proof of work and, in the handshake, before anything is known of
     * the peer.
     * <p>
     * The read loops of Connection and ConnectionHandshake close the connection after any exception: the start of the
     * next message is then unknown.
     *
     * @return The bytes of the next message, or null if the input stream ends before the first byte of a length prefix.
     * @throws InvalidProtocolBufferException if the length prefix has more than 5 bytes, if the length exceeds the size
     *                                        limit (before any byte of the message is read), or if the stream ends
     *                                        inside the length prefix.
     * @throws EOFException                   if the stream ends inside the message.
     */
    @Nullable
    public byte[] receiveNextMessage() throws IOException {
        // The handshake and the connection read the same input stream, each with its own NetworkEnvelopeSocket. The
        // decoder must therefore not take bytes after the length prefix: they would be missing for the next reader.
        // With a buffer of 1 byte it reads the prefix one byte at a time.
        CodedInputStream lengthPrefixInput = CodedInputStream.newInstance(inputStream, 1);
        // Reads the first byte of the prefix into the buffer of the decoder, where readRawVarint64 uses it
        if (lengthPrefixInput.isAtEnd()) {
            return null;
        }
        // readRawVarint32, which parseDelimitedFrom uses, keeps only the lower 32 bits, so a length of 2^32 would
        // become 0. readRawVarint64 keeps 64 bits, but it reads up to 10 bytes and drops higher bits of a prefix of 10
        // bytes. A prefix of at most 5 bytes has at most 35 bits, so the check of its size after the call makes the
        // decoded length exact. An invalid prefix of 6 to 10 bytes has been read then, but no byte of the message.
        long size = lengthPrefixInput.readRawVarint64();
        if (lengthPrefixInput.getTotalBytesRead() > MAX_LENGTH_PREFIX_SIZE) {
            throw new InvalidProtocolBufferException("The length prefix of the received message has more than " +
                    MAX_LENGTH_PREFIX_SIZE + " bytes.");
        }
        // Before we read or allocate anything for the message. We do not read past a larger message to reach the next
        // one: a node which follows the specification never sends it.
        if (size > maxSerializedSize) {
            throw new InvalidProtocolBufferException("The length prefix of the received message is " + size +
                    " bytes. The size limit is " + maxSerializedSize + " bytes.");
        }
        return readMessage((int) size);
    }

    /**
     * Parses the bytes of a message.
     * <p>
     * parseFrom(byte[]) reads only the bytes of the array: a field which is longer than the rest of the message fails.
     * It also rejects an end-group tag without a start-group tag. The parsed objects do not reference the array.
     * <p>
     * The nesting limit stays at the protobuf default of 100 levels. The deepest valid message has 9 levels, and the
     * memory of a parse grows with the number of sub-messages, not with their depth, so a lower limit would not reduce
     * it. A lower limit would only be a new protocol rule which a later message type could break.
     */
    public static bisq.network.protobuf.NetworkEnvelope parse(byte[] message) throws InvalidProtocolBufferException {
        return bisq.network.protobuf.NetworkEnvelope.parseFrom(message);
    }

    // We read all bytes of the message before we parse it. The parsed objects of a message can need many times more
    // memory than its bytes, up to about 22 times for many empty sub-messages. While a peer sends a message slowly, we
    // hold only its bytes. Only the length marks the end of a message, protobuf has no end marker.
    // readNBytes blocks until all bytes have arrived or the stream ends, and it reads no byte of the next message. Its
    // buffer grows while the bytes arrive, so a length prefix alone does not allocate the announced size. The socket
    // timeout applies to each single read.
    private byte[] readMessage(int size) throws IOException {
        byte[] message = inputStream.readNBytes(size);
        // The received part can be a valid message, for example if the stream ended at a field boundary
        if (message.length < size) {
            throw new EOFException("The input stream ended after " + message.length + " of " + size +
                    " bytes of a message.");
        }
        return message;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }
}
