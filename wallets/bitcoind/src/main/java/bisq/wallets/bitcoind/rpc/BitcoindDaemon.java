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

package bisq.wallets.bitcoind.rpc;

import bisq.wallets.bitcoind.rpc.calls.*;
import bisq.wallets.bitcoind.rpc.responses.BitcoindDecodeRawTransactionResponse;
import bisq.wallets.bitcoind.rpc.responses.BitcoindFinalizePsbtResponse;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.core.exceptions.RpcCallFailureException;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BitcoindDaemon {
    private final DaemonRpcClient rpcClient;

    public BitcoindDaemon(DaemonRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public void createOrLoadWallet(String walletName, Optional<String> passphrase) {
        createOrLoadWallet(walletName, passphrase, true, false, false);
    }

    public void createOrLoadLegacyWallet(String walletName, Optional<String> passphrase) {
        createOrLoadWallet(walletName, passphrase, false, false, false);
    }

    public void createOrLoadWatchOnlyWallet(String walletName) {
        createOrLoadWallet(walletName, Optional.empty(), true, true, true);
    }

    private void createOrLoadWallet(String walletName, Optional<String> passphrase, boolean descriptors, boolean disablePrivateKeys, boolean blank) {
        try {
            createWallet(walletName, passphrase.orElse(""), descriptors, disablePrivateKeys, blank);
        } catch (RpcCallFailureException e) {
            if (doesWalletExist(e)) {
                List<String> loadedWallets = listWallets();
                if (!loadedWallets.contains(walletName)) {
                    loadWallet(walletName);
                }
            }
        }
    }

    public String combinePsbt(List<String> txs) {
        var request = new BitcoindCombinePsbtRpcCall.Request(txs);
        var rpcCall = new BitcoindCombinePsbtRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindDecodeRawTransactionResponse decodeRawTransaction(String txInHex) {
        var request = new BitcoindDecodeRawTransactionRpcCall.Request(txInHex);
        var rpcCall = new BitcoindDecodeRawTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindFinalizePsbtResponse finalizePsbt(String psbt) {
        var request = new BitcoindFinalizePsbtRpcCall.Request(psbt);
        var rpcCall = new BitcoindFinalizePsbtRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<String> generateToAddress(int numberOfBlocksToMine, String addressOfMiner) {
        var request = BitcoindGenerateToAddressRpcCall.Request.builder()
                .nblocks(numberOfBlocksToMine)
                .address(addressOfMiner)
                .build();
        var rpcCall = new BitcoindGenerateToAddressRpcCall(request);
        String[] blockHashes = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(blockHashes);
    }

    public String getRawTransaction(String txId) {
        var request = new BitcoindGetRawTransactionRpcCall.Request(txId);
        var rpcCall = new BitcoindGetRawTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String getTxOutProof(List<String> txIds) {
        var request = new BitcoindGetTxOutProofRpcCall.Request(txIds);
        var rpcCall = new BitcoindGetTxOutProofRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<BitcoindGetZmqNotificationsResponse> getZmqNotifications() {
        var rpcCall = new BitcoindGetZmqNotificationsRpcCall();
        BitcoindGetZmqNotificationsResponse[] responses = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(responses);
    }

    public List<String> listWallets() {
        return listWalletsWithRpcClient(rpcClient);
    }

    private static List<String> listWalletsWithRpcClient(DaemonRpcClient rpcClient) {
        var rpcCall = new BitcoindListWalletsRpcCall();
        String[] wallets = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(wallets);
    }

    public String sendRawTransaction(String hexString) {
        var request = new BitcoindSendRawTransactionRpcCall.Request(hexString);
        var rpcCall = new BitcoindSendRawTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public void stop() {
        var rpcCall = new BitcoindStopRpcCall();
        rpcClient.invokeAndValidate(rpcCall);
    }

    public void unloadWallet(String walletName) {
        var request = new BitcoindUnloadWalletRpcCall.Request(walletName);
        var rpcCall = new BitcoindUnloadWalletRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public static boolean verifyRpcConfig(RpcConfig rpcConfig) {
        try {
            DaemonRpcClient rpcClient = RpcClientFactory.createLegacyDaemonRpcClient(rpcConfig);
            listWalletsWithRpcClient(rpcClient); // Makes a listwallets RPC call
            return true;
        } catch (InvalidRpcCredentialsException e) {
            return false;
        }
    }

    private boolean doesWalletExist(RpcCallFailureException e) {
        return e.getCause().getMessage().contains("Database already exists.");
    }

    private void createWallet(String walletName, String passphrase, boolean descriptors, boolean disablePrivateKeys, boolean blank) {
        var request = BitcoindCreateWalletRpcCall.Request.builder()
                .walletName(walletName)
                .descriptors(descriptors)
                .disablePrivateKeys(disablePrivateKeys)
                .blank(blank)
                .passphrase(passphrase)
                .build();

        var rpcCall = new BitcoindCreateWalletRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    private void loadWallet(String walletName) {
        var request = new BitcoindLoadWalletRpcCall.Request(walletName);
        var rpcCall = new BitcoindLoadWalletRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }
}
