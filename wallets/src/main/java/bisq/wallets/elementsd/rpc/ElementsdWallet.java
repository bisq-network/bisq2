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

import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.rpc.calls.BitcoindGetNewAddressRpcCall;
import bisq.wallets.bitcoind.rpc.calls.BitcoindSignMessageRpcCall;
import bisq.wallets.bitcoind.rpc.calls.BitcoindVerifyMessageRpcCall;
import bisq.wallets.bitcoind.rpc.calls.BitcoindWalletPassphraseRpcCall;
import bisq.wallets.elementsd.rpc.calls.*;
import bisq.wallets.elementsd.rpc.responses.*;
import bisq.wallets.rpc.RpcClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ElementsdWallet {
    private final RpcClient rpcClient;

    public ElementsdWallet(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public String claimPegin(String bitcoinTxId, String txOutProof) {
        var request = ElementsdClaimPegin.Request.builder()
                .bitcoinTxId(bitcoinTxId)
                .txOutProof(txOutProof)
                .build();
        var rpcCall = new ElementsdClaimPegin(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public double getLBtcBalance() {
        return getAssetBalance("bitcoin");
    }

    public double getAssetBalance(String assetLabel) {
        var request = new ElementsdGetBalanceRpcCall.Request(assetLabel);
        var rpcCall = new ElementsdGetBalanceRpcCall(request);
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

    public ElementsdGetPeginAddressResponse getPeginAddress() {
        var rpcCall = new ElementsdGetPeginAddressRpcCall();
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public ElementsdIssueAssetResponse issueAsset(double assetAmount, double tokenAmount) {
        var request = ElementsIssueAssetRpcCall.Request.builder()
                .assetAmount(assetAmount)
                .tokenAmount(tokenAmount)
                .build();
        var rpcCall = new ElementsIssueAssetRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<ElementsdListTransactionsResponseEntry> listTransactions(int count) {
        var request = ElementsdListTransactionsRpcCall.Request.builder()
                .count(count)
                .build();
        var rpcCall = new ElementsdListTransactionsRpcCall(request);
        ElementsdListTransactionsResponseEntry[] response = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(response);
    }

    public List<ElementsdListUnspentResponseEntry> listUnspent() {
        var rpcCall = new ElementsdListUnspentRpcCall();
        ElementsdListUnspentResponseEntry[] response = rpcClient.invokeAndValidate(rpcCall);
        return Arrays.asList(response);
    }

    public String sendLBtcToAddress(String address, double amount) {
        return sendAssetToAddress("bitcoin", address, amount);
    }

    public String sendAssetToAddress(String assetLabel, String address, double amount) {
        var request = ElementsdSendToAddressRpcCall.Request.builder()
                .assetLabel(assetLabel)
                .address(address)
                .amount(amount)
                .build();
        var rpcCall = new ElementsdSendToAddressRpcCall(request);
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
}
