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

import bisq.common.encoding.Hex;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.responses.BitcoindDecodeRawTransactionResponse;

public class BitcoindRawTxProcessor implements ZmqRawTxProcessor {

    private final BitcoindDaemon daemon;
    private final ZmqListeners listeners;

    public BitcoindRawTxProcessor(BitcoindDaemon daemon, ZmqListeners listeners) {
        this.daemon = daemon;
        this.listeners = listeners;
    }

    @Override
    public void processRawTx(byte[] serializedTx, byte[] sequenceNumber) {
        String txInHex = Hex.encode(serializedTx);
        BitcoindDecodeRawTransactionResponse rawTransaction = daemon.decodeRawTransaction(txInHex);
        listeners.fireTxOutputAddressesListeners(rawTransaction);
        listeners.fireTxIdInputListeners(rawTransaction);
    }
}
