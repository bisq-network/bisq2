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

package bisq.network.p2p.node.envelope.parser;

public class ProtoBufMessageLengthParser {
    public static final int STILL_PARSING_MESSAGE_LENGTH = -1;
    private final ProtoBufInputStream protoBufInputStream;

    private int readLengthBytes = 0;
    private long currentMessageLength = 0;

    public ProtoBufMessageLengthParser(ProtoBufInputStream protoBufInputStream) {
        this.protoBufInputStream = protoBufInputStream;
    }

    public long parseMessageLength() {
        byte readByte = protoBufInputStream.read();
        long thisNumber = (readByte & 0x7f);

        int bitsToShift = readLengthBytes * 7;
        thisNumber = thisNumber << bitsToShift;

        currentMessageLength = currentMessageLength ^ thisNumber;

        boolean needToReadMore = isContinuationBitSet(readByte);
        if (needToReadMore) {
            readLengthBytes++;
            return STILL_PARSING_MESSAGE_LENGTH;

        } else {
            readLengthBytes = 0;
            long parsedMessageLength = currentMessageLength;
            currentMessageLength = 0;
            return parsedMessageLength;
        }
    }

    private boolean isContinuationBitSet(byte readByte) {
        int continuationBit = (readByte & 0x80) >> 7;
        return continuationBit == 1;
    }
}
