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

package bisq.wallets.elementsd.rpc;

import bisq.common.encoding.Hex;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.bitcoind.zmq.ZmqRawTxProcessor;
import bisq.wallets.elementsd.rpc.responses.ElementsdDecodeRawTransactionResponse;

public class ElementsdRawTxProcessor implements ZmqRawTxProcessor {

    private final ElementsdDaemon daemon;
    private final ElementsdWallet wallet;
    private final ZmqListeners listeners;

    public ElementsdRawTxProcessor(ElementsdDaemon daemon, ElementsdWallet wallet, ZmqListeners listeners) {
        this.daemon = daemon;
        this.wallet = wallet;
        this.listeners = listeners;
    }

    @Override
    public void processRawTx(byte[] serializedTx, byte[] sequenceNumber) {
        String txInHex = Hex.encode(serializedTx);
        String unblindedTxInHex = wallet.unblindRawTransaction(txInHex);
        ElementsdDecodeRawTransactionResponse rawTransaction = daemon.decodeRawTransaction(unblindedTxInHex);

        listeners.fireTxOutputAddressesListeners(rawTransaction);
        listeners.fireTxIdInputListeners(rawTransaction);
    }
}
