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
     * Reads the next message. If its length prefix is longer than 5 bytes or exceeds the size limit, it throws
     * InvalidProtocolBufferException before it reads the message.
     *
     * @return The next message, or null if the input stream ends before the next message.
     */
    public bisq.network.protobuf.NetworkEnvelope receiveNextEnvelope() throws IOException {
        // A buffer of 1 byte keeps the decoder from reading bytes after the length prefix. With a larger buffer, these
        // bytes would be missing from the message, which we read from the input stream.
        CodedInputStream lengthPrefixInput = CodedInputStream.newInstance(inputStream, 1);
        if (lengthPrefixInput.isAtEnd()) {
            return null;
        }
        // readRawVarint32, which parseDelimitedFrom uses, keeps only the lower 32 bits, so a length of 2^32 would
        // become 0. readRawVarint64 keeps 64 bits, but it drops higher bits of a prefix of 10 bytes. A prefix of at most
        // 5 bytes has at most 35 bits, so the check of its size makes the decoded length exact.
        long size = lengthPrefixInput.readRawVarint64();
        if (lengthPrefixInput.getTotalBytesRead() > MAX_LENGTH_PREFIX_SIZE) {
            throw new InvalidProtocolBufferException("The length prefix of the received message has more than " +
                    MAX_LENGTH_PREFIX_SIZE + " bytes.");
        }
        if (size > maxSerializedSize) {
            throw new InvalidProtocolBufferException("The length prefix of the received message is " + size +
                    " bytes. The size limit is " + maxSerializedSize + " bytes.");
        }

        // We read all bytes of the message before we parse it. The parsed objects of a message can need many times
        // more memory than its bytes. While a peer sends a message slowly, we hold only its bytes.
        byte[] message = inputStream.readNBytes((int) size);
        if (message.length < size) {
            throw new EOFException("The input stream ended after " + message.length + " of " + size +
                    " bytes of a message.");
        }
        return bisq.network.protobuf.NetworkEnvelope.parseFrom(message);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }
}
