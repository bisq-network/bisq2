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
import bisq.wallets.bitcoind.rpc.calls.requests.BitcoindImportDescriptorRequestEntry;
import bisq.wallets.bitcoind.rpc.calls.requests.BitcoindImportMultiRequest;
import bisq.wallets.bitcoind.rpc.psbt.BitcoindPsbtInput;
import bisq.wallets.bitcoind.rpc.responses.*;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.core.rpc.WalletRpcClient;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BitcoindWallet {

    private static final long DEFAULT_WALLET_TIMEOUT = TimeUnit.SECONDS.toSeconds(15);
    private final BitcoindDaemon daemon;
    private final WalletRpcClient rpcClient;

    public BitcoindWallet(BitcoindDaemon daemon, RpcConfig rpcConfig, Path walletPath) throws MalformedURLException {
        this.daemon = daemon;
        this.rpcClient = RpcClientFactory.createWalletRpcClient(rpcConfig, walletPath);
    }

    public void initialize(Optional<String> passphrase) {
        Path walletPath = rpcClient.getWalletPath();
        daemon.createOrLoadWallet(walletPath, passphrase);
    }

    public void shutdown() {
        Path walletPath = rpcClient.getWalletPath();
        daemon.unloadWallet(walletPath);
    }

    public BitcoindAddOrCreateMultiSigAddressResponse createMultiSig(int nRequired, List<String> keys) {
        var request = BitcoindCreateMultiSigRpcCall.Request.builder()
                .nRequired(nRequired)
                .keys(keys)
                .build();
        var rpcCall = new BitcoindCreateMultiSigRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindAddOrCreateMultiSigAddressResponse addMultiSigAddress(int nRequired, List<String> keys) {
        var request = BitcoindAddMultiSigAddressRpcCall.Request.builder()
                .nRequired(nRequired)
                .keys(keys)
                .build();
        var rpcCall = new BitcoindAddMultiSigAddressRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindGetAddressInfoResponse getAddressInfo(String address) {
        var request = new BitcoindGetAddressInfoRpcCall.Request(address);
        var rpcCall = new BitcoindGetAddressInfoRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public double getBalance() {
        var rpcCall = new BitcoindGetBalancesRpcCall();
        BitcoindGetBalancesResponse response = rpcClient.invokeAndValidate(rpcCall);
        BitcoindGetMineBalancesResponse mineBalancesResponse = response.getMine();
        return mineBalancesResponse.getTrusted() + mineBalancesResponse.getUntrustedPending();
    }

    public BitcoindGetDescriptorInfoResponse getDescriptorInfo(String descriptor) {
        var request = new BitcoindGetDescriptorInfoRpcCall.Request(descriptor);
        var rpcCall = new BitcoindGetDescriptorInfoRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String getNewAddress(AddressType addressType, String label) {
        var request = BitcoindGetNewAddressRpcCall.Request.builder()
                .addressType(addressType.getName())
                .label(label)
                .build();
        var rpcCall = new BitcoindGetNewAddressRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<BitcoindImportDescriptorResponseEntry> importDescriptors(
            List<BitcoindImportDescriptorRequestEntry> requests
    ) {
        var request = new BitcoindImportDescriptorsRpcCall.Request(
                requests.toArray(new BitcoindImportDescriptorRequestEntry[0])
        );
        var rpcCall = new BitcoindImportDescriptorsRpcCall(request);
        return Arrays.asList(
                rpcClient.invokeAndValidate(rpcCall)
        );
    }

    public void importAddress(String address, String label) {
        var request = BitcoindImportAddressRpcCall.Request.builder()
                .address(address)
                .label(label)
                .build();
        var rpcCall = new BitcoindImportAddressRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public List<BitcoinImportMultiEntryResponse> importMulti(List<BitcoindImportMultiRequest> requests) {
        var request = BitcoindImportMultiRpcCall.Request.builder()
                .requests(requests)
                .build();
        var rpcCall = new BitcoindImportMultiRpcCall(request);
        BitcoinImportMultiEntryResponse[] response = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(response);
    }

    public BitcoindListDescriptorResponse listDescriptors() {
        var rpcCall = new BitcoindListDescriptorsRpcCall();
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<BitcoindListTransactionsResponseEntry> listTransactions(int count) {
        var request = BitcoindListTransactionsRpcCall.Request.builder()
                .count(count)
                .build();
        var rpcCall = new BitcoindListTransactionsRpcCall(request);
        BitcoindListTransactionsResponseEntry[] response = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(response);
    }

    public List<BitcoindListUnspentResponseEntry> listUnspent() {
        var rpcCall = new BitcoindListUnspentRpcCall();
        BitcoindListUnspentResponseEntry[] response = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(response);
    }

    public String sendToAddress(Optional<String> passphrase, String address, double amount) {
        walletPassphrase(passphrase);

        var request = BitcoindSendToAddressRpcCall.Request.builder()
                .address(address)
                .amount(amount)
                .build();
        var rpcCall = new BitcoindSendToAddressRpcCall(request);
        String txId = rpcClient.invokeAndValidate(rpcCall);

        if (passphrase.isPresent()) {
            walletLock();
        }
        return txId;
    }

    public String signMessage(Optional<String> walletPasshrase, String address, String message) {
        walletPassphrase(walletPasshrase);

        var request = BitcoindSignMessageRpcCall.Request.builder()
                .address(address)
                .message(message)
                .build();
        var rpcCall = new BitcoindSignMessageRpcCall(request);
        String signature = rpcClient.invokeAndValidate(rpcCall);

        if (walletPasshrase.isPresent()) {
            walletLock();
        }
        return signature;
    }

    public boolean verifyMessage(String address, String signature, String message) {
        var request = BitcoindVerifyMessageRpcCall.Request.builder()
                .address(address)
                .signature(signature)
                .message(message)
                .build();
        var rpcCall = new BitcoindVerifyMessageRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindWalletCreateFundedPsbtResponse walletCreateFundedPsbt(List<BitcoindPsbtInput> inputs,
                                                                         Map<String, Double> outputs,
                                                                         Map<String, Double> options) {
        var request = BitcoindWalletCreateFundedPsbtRpcCall.Request.builder()
                .inputs(inputs)
                .outputs(outputs)
                .options(options)
                .build();
        var rpcCall = new BitcoindWalletCreateFundedPsbtRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public void walletLock() {
        var rpcCall = new BitcoindWalletLockRpcCall();
        rpcClient.invokeAndValidate(rpcCall);
    }

    private void walletPassphrase(Optional<String> passphrase) {
        walletPassphrase(rpcClient, passphrase);
    }

    public static void walletPassphrase(WalletRpcClient rpcClient, Optional<String> passphrase) {
        String passphraseString = passphrase.orElse("");
        if (passphraseString.isEmpty()) {
            return;
        }

        var request = BitcoindWalletPassphraseRpcCall.Request.builder()
                .passphrase(passphrase.get())
                .timeout(DEFAULT_WALLET_TIMEOUT)
                .build();
        var rpcCall = new BitcoindWalletPassphraseRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindWalletProcessPsbtResponse walletProcessPsbt(Optional<String> passphrase, String psbt) {
        walletPassphrase(passphrase);

        var request = new BitcoindWalletProcessPsbtRpcCall.Request(psbt);
        var rpcCall = new BitcoindWalletProcessPsbtRpcCall(request);
        BitcoindWalletProcessPsbtResponse response = rpcClient.invokeAndValidate(rpcCall);

        if (passphrase.isPresent()) {
            walletLock();
        }

        return response;
    }
}
