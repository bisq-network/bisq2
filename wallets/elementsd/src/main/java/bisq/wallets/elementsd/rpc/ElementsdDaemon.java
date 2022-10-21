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

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.elementsd.rpc.calls.ElementsdDecodeRawTransactionRpcCall;
import bisq.wallets.elementsd.rpc.calls.ElementsdStopRpcCall;
import bisq.wallets.elementsd.rpc.responses.ElementsdDecodeRawTransactionResponse;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ElementsdDaemon {

    private final DaemonRpcClient rpcClient;
    private final BitcoindDaemon bitcoindDaemon;

    public ElementsdDaemon(DaemonRpcClient rpcClient) {
        this.rpcClient = rpcClient;
        bitcoindDaemon = new BitcoindDaemon(rpcClient);
    }

    public void createOrLoadWallet(String walletName, Optional<String> passphrase) {
        bitcoindDaemon.createOrLoadWallet(walletName, passphrase);
    }

    public ElementsdDecodeRawTransactionResponse decodeRawTransaction(String txInHex) {
        var request = new ElementsdDecodeRawTransactionRpcCall.Request(txInHex);
        var rpcCall = new ElementsdDecodeRawTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<String> generateToAddress(int numberOfBlocksToMine, String addressOfMiner) {
        return bitcoindDaemon.generateToAddress(numberOfBlocksToMine, addressOfMiner);
    }

    public String getRawTransaction(String txId) {
        return bitcoindDaemon.getRawTransaction(txId);
    }

    public String getTxOutProof(List<String> txIds) {
        return bitcoindDaemon.getTxOutProof(txIds);
    }

    public List<BitcoindGetZmqNotificationsResponse> getZmqNotifications() {
        return bitcoindDaemon.getZmqNotifications();
    }

    public List<String> listWallets() {
        return bitcoindDaemon.listWallets();
    }

    public String sendRawTransaction(String hexString) {
        return bitcoindDaemon.sendRawTransaction(hexString);
    }

    public void stop() {
        var rpcCall = new ElementsdStopRpcCall();
        rpcClient.invokeAndValidate(rpcCall);
    }

    public void unloadWallet(String walletName) {
        bitcoindDaemon.unloadWallet(walletName);
    }
}
