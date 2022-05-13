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
import bisq.wallets.bitcoind.rpc.psbt.BitcoindPsbtInput;
import bisq.wallets.bitcoind.rpc.psbt.BitcoindPsbtOptions;
import bisq.wallets.bitcoind.rpc.psbt.BitcoindPsbtOutput;
import bisq.wallets.bitcoind.rpc.responses.*;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.WalletRpcClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BitcoindWallet {

    public static final long DEFAULT_WALLET_TIMEOUT = TimeUnit.HOURS.toSeconds(24);
    private final WalletRpcClient rpcClient;

    public BitcoindWallet(WalletRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public BitcoindAddMultisigAddressResponse addMultiSigAddress(int nRequired, List<String> keys) {
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

    public String getNewAddress(AddressType addressType, String label) {
        var request = BitcoindGetNewAddressRpcCall.Request.builder()
                .addressType(addressType.getName())
                .label(label)
                .build();
        var rpcCall = new BitcoindGetNewAddressRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public void importAddress(String address, String label) {
        var request = BitcoindImportAddressRpcCall.Request.builder()
                .address(address)
                .label(label)
                .build();
        var rpcCall = new BitcoindImportAddressRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
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

    public String sendToAddress(String address, double amount) {
        var request = BitcoindSendToAddressRpcCall.Request.builder()
                .address(address)
                .amount(amount)
                .build();
        var rpcCall = new BitcoindSendToAddressRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String signMessage(String address, String message) {
        var request = BitcoindSignMessageRpcCall.Request.builder()
                .address(address)
                .message(message)
                .build();
        var rpcCall = new BitcoindSignMessageRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
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
                                                                         BitcoindPsbtOutput psbtOutput,
                                                                         int lockTime,
                                                                         BitcoindPsbtOptions psbtOptions) {
        var request = BitcoindWalletCreateFundedPsbtRpcCall.Request.builder()
                .inputs(inputs)
                .outputs(psbtOutput.toPsbtOutputObject())
                .lockTime(lockTime)
                .options(psbtOptions)
                .build();
        var rpcCall = new BitcoindWalletCreateFundedPsbtRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public void walletPassphrase(Optional<String> passphrase, long timeout) {
        if (passphrase.isEmpty()) {
            return;
        }

        var request = BitcoindWalletPassphraseRpcCall.Request.builder()
                .passphrase(passphrase.get())
                .timeout(timeout)
                .build();
        var rpcCall = new BitcoindWalletPassphraseRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public BitcoindWalletProcessPsbtResponse walletProcessPsbt(String psbt) {
        var request = new BitcoindWalletProcessPsbtRpcCall.Request(psbt);
        var rpcCall = new BitcoindWalletProcessPsbtRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }
}
