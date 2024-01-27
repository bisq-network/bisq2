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

import bisq.wallets.bitcoind.rpc.responses.AbstractDecodeRawTransactionResponse;
import bisq.wallets.bitcoind.zmq.listeners.NewBlockMinedListener;
import bisq.wallets.bitcoind.zmq.listeners.TransactionOutputAddressesListener;
import bisq.wallets.bitcoind.zmq.listeners.TxIdInInputListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class ZmqListeners {
    @Getter
    private final List<NewBlockMinedListener> newBlockMinedListeners = new CopyOnWriteArrayList<>();
    @Getter
    private final List<TransactionOutputAddressesListener> txOutputAddressesListeners = new CopyOnWriteArrayList<>();
    @Getter
    private final List<TxIdInInputListener> txIdInInputListeners = new CopyOnWriteArrayList<>();

    public <T extends AbstractDecodeRawTransactionResponse<?, ?>> void fireTxOutputAddressesListeners(T rawTransaction) {
        Set<String> addressesInOutput = rawTransaction.getVout()
                .stream()
                .flatMap(vout -> vout.getAddresses().stream())
                .collect(Collectors.toSet());
        txOutputAddressesListeners.forEach(listener -> {
            try {
                listener.onNewTransaction(addressesInOutput);
            } catch (Exception e) {
                log.error("Calling onNewTransaction at listener {} failed", listener, e);
            }
        });
    }

    public <T extends AbstractDecodeRawTransactionResponse<?, ?>> void fireTxIdInputListeners(T rawTransaction) {
        rawTransaction.getVin().forEach(vin -> {
            String txId = vin.getTxId();
            if (txId != null) {
                txIdInInputListeners.forEach(listener -> {
                    try {
                        listener.onTxIdInInput(txId);
                    } catch (Exception e) {
                        log.error("Calling onTxIdInInput at listener {} failed", listener, e);
                    }
                });
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

    public void unregisterNewBlockMinedListener(NewBlockMinedListener listener) {
        newBlockMinedListeners.remove(listener);
    }

    public void registerTxOutputAddressesListener(TransactionOutputAddressesListener listener) {
        txOutputAddressesListeners.add(listener);
    }

    public void unregisterTxOutputAddressesListener(TransactionOutputAddressesListener listener) {
        txOutputAddressesListeners.remove(listener);
    }

    public void registerTransactionIdInInputListener(TxIdInInputListener listener) {
        txIdInInputListeners.add(listener);
    }

    public void unregisterTransactionIdInInputListener(TxIdInInputListener listener) {
        txIdInInputListeners.remove(listener);
    }
}
