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

import bisq.wallets.bitcoind.rpc.responses.BitcoindDecodeRawTransactionResponse;
import bisq.wallets.bitcoind.zeromq.listeners.NewBlockMinedListener;
import bisq.wallets.bitcoind.zeromq.listeners.TransactionOutputAddressesListener;
import bisq.wallets.bitcoind.zeromq.listeners.TxIdInInputListener;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BitcoindZeroMqListeners {
    @Getter
    private final List<NewBlockMinedListener> newBlockMinedListeners = new CopyOnWriteArrayList<>();
    @Getter
    private final List<TransactionOutputAddressesListener> txOutputAddressesListeners = new CopyOnWriteArrayList<>();
    @Getter
    private final List<TxIdInInputListener> txIdInInputListeners = new CopyOnWriteArrayList<>();

    void fireTxOutputAddressesListeners(BitcoindDecodeRawTransactionResponse rawTransaction) {
        Set<String> addressesInOutput = rawTransaction.getVout()
                .stream()
                .map(vout -> vout.getScriptPubKey().getAddress())
                .collect(Collectors.toSet());
        txOutputAddressesListeners.forEach(listener -> listener.onNewTransaction(addressesInOutput));
    }

    void fireTxIdInputListeners(BitcoindDecodeRawTransactionResponse rawTransaction) {
        rawTransaction.getVin().forEach(vin -> {
            String txId = vin.getTxId();
            if (txId != null) {
                txIdInInputListeners.forEach(listener -> listener.onTxIdInInput(txId));
            }
        });
    }

    public void clearAll() {
        newBlockMinedListeners.clear();
        txOutputAddressesListeners.clear();
        txIdInInputListeners.clear();
    }

    public void registerNewBlockMinedListener(NewBlockMinedListener listener) {
        newBlockMinedListeners.add(listener);
    }

    public void removeNewBlockMinedListener(NewBlockMinedListener listener) {
        newBlockMinedListeners.remove(listener);
    }

    public void registerTxOutputAddressesListener(TransactionOutputAddressesListener listener) {
        txOutputAddressesListeners.add(listener);
    }

    public void removeTxOutputAddressesListener(TransactionOutputAddressesListener listener) {
        txOutputAddressesListeners.remove(listener);
    }

    public void registerTransactionIdInInputListener(TxIdInInputListener listener) {
        txIdInInputListeners.add(listener);
    }

    public void removeTransactionIdInInputListener(TxIdInInputListener listener) {
        txIdInInputListeners.remove(listener);
    }
}
