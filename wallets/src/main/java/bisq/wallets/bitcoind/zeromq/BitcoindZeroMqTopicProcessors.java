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

package bisq.wallets.bitcoind.zeromq;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.responses.BitcoindDecodeRawTransactionResponse;
import com.google.common.io.BaseEncoding;

public class BitcoindZeroMqTopicProcessors {

    private final BitcoindDaemon bitcoindDaemon;
    private final BitcoindZeroMqListeners listeners;

    public BitcoindZeroMqTopicProcessors(BitcoindDaemon bitcoindDaemon, BitcoindZeroMqListeners listeners) {
        this.bitcoindDaemon = bitcoindDaemon;
        this.listeners = listeners;
    }

    public void process(BitcoindZeroMqMessage zeroMqMessage) {
        byte[] secondPart = zeroMqMessage.secondPart();
        byte[] thirdPart = zeroMqMessage.thirdPart();
        switch (zeroMqMessage.topic()) {
            case TOPIC_HASHBLOCK -> processHashBlock(secondPart, thirdPart);
            case TOPIC_RAWTX -> processRawTx(secondPart, thirdPart);
        }
    }

    private void processHashBlock(byte[] blockHash, byte[] sequenceNumber) {
        String blockHashInHex = bytesToHexString(blockHash);
        listeners.getNewBlockMinedListeners().forEach(listener -> listener.onNewBlock(blockHashInHex));
    }

    private void processRawTx(byte[] serializedTx, byte[] sequenceNumber) {
        String txInHex = bytesToHexString(serializedTx);
        BitcoindDecodeRawTransactionResponse rawTransaction = bitcoindDaemon.decodeRawTransaction(txInHex);

        listeners.fireTxOutputAddressesListeners(rawTransaction);
        listeners.fireTxIdInputListeners(rawTransaction);
    }

    private String bytesToHexString(byte[] bytes) {
        return BaseEncoding.base16()
                .encode(bytes)
                .toLowerCase();
    }
}
