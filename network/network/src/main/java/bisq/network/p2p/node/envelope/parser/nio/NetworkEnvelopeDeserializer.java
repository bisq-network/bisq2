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

package bisq.network.p2p.node.envelope.parser.nio;

import bisq.network.p2p.node.envelope.parser.ProtoBufMessageLengthParser;
import bisq.network.protobuf.NetworkEnvelope;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.*;

@Slf4j
public class NetworkEnvelopeDeserializer {

    private final ByteBuffer byteBuffer;
    private final ProtoBufMessageLengthParser messageLengthParser;
    private boolean parsingMessage = false;
    private long currentMessageLength = 0;
    private int parsedMessageLength = 0;
    private byte[] currentProtobufMessage;

    private final Queue<bisq.network.p2p.message.NetworkEnvelope> parsedNetworkEnvelopes = new LinkedList<>();

    public NetworkEnvelopeDeserializer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        var protoBufInputStream = new NioProtoBufInputStream(byteBuffer);
        this.messageLengthParser = new ProtoBufMessageLengthParser(protoBufInputStream);
    }

    public void readFromByteBuffer() {
        while (byteBuffer.hasRemaining()) {
            if (!parsingMessage) {
                parseMessageLength();

            } else {
                if (inMiddleOfParsingMessage()) {
                    parsePartialMessage();

                    if (isMessageComplete()) {
                        tryToParseProtoBufMessage();
                    }

                } else {
                    parseNewIncomingMessage();
                }
            }
        }
    }

    private void parseMessageLength() {
        long messageLength = messageLengthParser.parseMessageLength();
        if (isMessageLengthParsed(messageLength)) {
            currentMessageLength = messageLength;
            parsedMessageLength = 0;
            parsingMessage = true;
        }
    }

    private boolean inMiddleOfParsingMessage() {
        return currentProtobufMessage != null;
    }

    private void parsePartialMessage() {
        long lengthOfMissingMessage = currentMessageLength - parsedMessageLength;

        if (byteBuffer.remaining() >= lengthOfMissingMessage) {
            parsePartialMessageFromByteBuffer((int) lengthOfMissingMessage);
        } else {
            int lengthOfMessageInBuffer = byteBuffer.remaining();
            parsePartialMessageFromByteBuffer(lengthOfMessageInBuffer);
        }
    }

    private boolean isMessageComplete() {
        return parsedMessageLength == currentMessageLength;
    }

    private void tryToParseProtoBufMessage() {
        try {
            NetworkEnvelope message = NetworkEnvelope.parseFrom(currentProtobufMessage);
            bisq.network.p2p.message.NetworkEnvelope
                    networkEnvelope = bisq.network.p2p.message.NetworkEnvelope.fromProto(message);
            networkEnvelope.verifyVersion();
            parsedNetworkEnvelopes.add(networkEnvelope);

        } catch (InvalidProtocolBufferException e) {
            log.error("Couldn't parse protocol buffer message.", e);
        } finally {
            resetState();
        }
    }

    private void parseNewIncomingMessage() {
        currentProtobufMessage = new byte[(int) currentMessageLength];
        parsedMessageLength = 0;

        if (isFullMessageInByteBuffer()) {
            byteBuffer.get(currentProtobufMessage, parsedMessageLength, (int) currentMessageLength);
            tryToParseProtoBufMessage();

        } else {
            int lengthOfMessageInBuffer = byteBuffer.remaining();
            parsePartialMessageFromByteBuffer(lengthOfMessageInBuffer);

            if (isMessageComplete()) {
                tryToParseProtoBufMessage();
            }
        }
    }

    private void parsePartialMessageFromByteBuffer(int length) {
        byteBuffer.get(currentProtobufMessage, parsedMessageLength, length);
        parsedMessageLength += length;
    }

    private void resetState() {
        parsingMessage = false;
        currentMessageLength = 0;
        parsedMessageLength = 0;
        currentProtobufMessage = null;
    }

    public List<bisq.network.p2p.message.NetworkEnvelope> getAllNetworkEnvelopes() {
        if (parsedNetworkEnvelopes.isEmpty()) {
            return Collections.emptyList();
        }

        List<bisq.network.p2p.message.NetworkEnvelope> networkEnvelopes = new ArrayList<>(parsedNetworkEnvelopes.size());
        networkEnvelopes.addAll(parsedNetworkEnvelopes);

        parsedNetworkEnvelopes.clear();

        return networkEnvelopes;
    }

    private boolean isMessageLengthParsed(long messageLength) {
        return messageLength != ProtoBufMessageLengthParser.STILL_PARSING_MESSAGE_LENGTH;
    }

    private boolean isFullMessageInByteBuffer() {
        int messageLength = ((int) currentMessageLength) - parsedMessageLength;
        return byteBuffer.remaining() >= messageLength;
    }
}
