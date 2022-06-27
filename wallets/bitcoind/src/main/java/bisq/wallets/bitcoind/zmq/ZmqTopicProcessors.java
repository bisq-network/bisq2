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

package bisq.wallets.bitcoind.zmq;

import com.google.common.io.BaseEncoding;

public class ZmqTopicProcessors {

    private final ZmqListeners listeners;
    private final ZmqRawTxProcessor rawTxProcessor;

    public ZmqTopicProcessors(ZmqRawTxProcessor rawTxProcessor, ZmqListeners listeners) {
        this.rawTxProcessor = rawTxProcessor;
        this.listeners = listeners;
    }

    public void process(BitcoindZmqMessage zeroMqMessage) {
        byte[] secondPart = zeroMqMessage.getSecondPart();
        byte[] thirdPart = zeroMqMessage.getThirdPart();
        switch (zeroMqMessage.getTopic()) {
            case TOPIC_HASHBLOCK:
                processHashBlock(secondPart, thirdPart);
                break;
            case TOPIC_RAWTX:
                rawTxProcessor.processRawTx(secondPart, thirdPart);
                break;
        }
    }

    private void processHashBlock(byte[] blockHash, byte[] sequenceNumber) {
        String blockHashInHex = bytesToHexString(blockHash);
        listeners.getNewBlockMinedListeners().forEach(listener -> listener.onNewBlock(blockHashInHex));
    }

    private String bytesToHexString(byte[] bytes) {
        return BaseEncoding.base16()
                .encode(bytes)
                .toLowerCase();
    }
}
