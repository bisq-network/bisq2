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

import bisq.network.p2p.message.NetworkEnvelope;
import bisq.network.p2p.node.envelope.parser.nio.NetworkEnvelopeDeserializer;
import bisq.network.p2p.node.envelope.parser.nio.ProtoBufMessageLengthWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;

@Slf4j
public class NetworkEnvelopeSocketChannel implements Closeable {
    public static final int BYTE_BUFFER_SIZE = 1024;

    private static final int END_OF_STREAM = -1;

    @Getter
    private final SocketChannel socketChannel;
    private final ByteBuffer byteBuffer;
    private final NetworkEnvelopeDeserializer networkEnvelopeDeserializer;


    public NetworkEnvelopeSocketChannel(SocketChannel socketChannel) {
        this(socketChannel, BYTE_BUFFER_SIZE);
    }

    public NetworkEnvelopeSocketChannel(SocketChannel socketChannel, int byteBufferSize) {
        this.socketChannel = socketChannel;
        this.byteBuffer = ByteBuffer.allocate(byteBufferSize);
        this.networkEnvelopeDeserializer = new NetworkEnvelopeDeserializer(byteBuffer);
    }

    public void send(NetworkEnvelope networkEnvelope) throws IOException {
        bisq.network.protobuf.NetworkEnvelope proto = networkEnvelope.completeProto();
        byte[] protoInBytes = proto.toByteArray();
        int messageLength = protoInBytes.length;

        ByteBuffer byteBuffer1 = ByteBuffer.allocate(messageLength + 10);
        ProtoBufMessageLengthWriter.writeToBuffer(messageLength, byteBuffer1);

        byteBuffer1.put(protoInBytes);
        byteBuffer1.flip();

        socketChannel.write(byteBuffer1);
    }

    public List<NetworkEnvelope> receiveNetworkEnvelopes() throws IOException {
        byteBuffer.clear();

        int numberOfReadBytes = socketChannel.read(byteBuffer);
        if (numberOfReadBytes == END_OF_STREAM) {
            socketChannel.close();
            return Collections.emptyList();
        }

        byteBuffer.flip();
        networkEnvelopeDeserializer.readFromByteBuffer();

        List<NetworkEnvelope>
                allNetworkEnvelopes = networkEnvelopeDeserializer.getAllNetworkEnvelopes();
        allNetworkEnvelopes.forEach(NetworkEnvelope::verifyVersion);

        return allNetworkEnvelopes;
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }
}
