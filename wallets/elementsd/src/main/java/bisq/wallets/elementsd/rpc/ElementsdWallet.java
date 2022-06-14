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

import bisq.wallets.bitcoind.rpc.calls.*;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.WalletRpcClient;
import bisq.wallets.elementsd.rpc.calls.*;
import bisq.wallets.elementsd.rpc.responses.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ElementsdWallet {
    public static final long DEFAULT_WALLET_TIMEOUT = TimeUnit.MINUTES.toSeconds(1);
    private final WalletRpcClient rpcClient;

    public ElementsdWallet(WalletRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public String claimPegin(Optional<String> passphrase, String bitcoinTxId, String txOutProof) {
        walletPassphrase(passphrase, DEFAULT_WALLET_TIMEOUT);

        var request = ElementsdClaimPeginRpcCall.Request.builder()
                .bitcoinTxId(bitcoinTxId)
                .txOutProof(txOutProof)
                .build();
        var rpcCall = new ElementsdClaimPeginRpcCall(request);
        String result = rpcClient.invokeAndValidate(rpcCall);

        walletLock();
        return result;
    }

    public double getLBtcBalance() {
        return getAssetBalance("bitcoin");
    }

    public double getAssetBalance(String assetLabel) {
        var rpcCall = new ElementsdGetBalancesRpcCall();
        ElementsdGetBalancesResponse response = rpcClient.invokeAndValidate(rpcCall);
        ElementsdGetMineBalancesResponse mineBalancesResponse = response.getMine();

        double trustedBalance = mineBalancesResponse.getTrusted().getOrDefault(assetLabel, 0.);
        double pendingBalance = mineBalancesResponse.getUntrustedPending().getOrDefault(assetLabel, 0.);
        return trustedBalance + pendingBalance;
    }

    public ElementsdGetAddressInfoResponse getAddressInfo(String address) {
        var request = new ElementsdGetAddressInfoRpcCall.Request(address);
        var rpcCall = new ElementsdGetAddressInfoRpcCall(request);
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

    public ElementsdIssueAssetResponse issueAsset(Optional<String> passphrase, double assetAmount, double tokenAmount) {
        walletPassphrase(passphrase, DEFAULT_WALLET_TIMEOUT);

        var request = ElementsIssueAssetRpcCall.Request.builder()
                .assetAmount(assetAmount)
                .tokenAmount(tokenAmount)
                .build();
        var rpcCall = new ElementsIssueAssetRpcCall(request);
        ElementsdIssueAssetResponse response = rpcClient.invokeAndValidate(rpcCall);

        walletLock();
        return response;
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

    public String sendLBtcToAddress(Optional<String> passphrase, String address, double amount) {
        return sendAssetToAddress(passphrase, "bitcoin", address, amount);
    }

    public String sendAssetToAddress(Optional<String> passphrase, String assetLabel, String address, double amount) {
        walletPassphrase(passphrase, DEFAULT_WALLET_TIMEOUT);

        var request = ElementsdSendToAddressRpcCall.Request.builder()
                .assetLabel(assetLabel)
                .address(address)
                .amount(amount)
                .build();
        var rpcCall = new ElementsdSendToAddressRpcCall(request);
        String txId = rpcClient.invokeAndValidate(rpcCall);

        walletLock();
        return txId;
    }

    public String signMessage(Optional<String> passphrase, String address, String message) {
        walletPassphrase(passphrase, DEFAULT_WALLET_TIMEOUT);

        var request = BitcoindSignMessageRpcCall.Request.builder()
                .address(address)
                .message(message)
                .build();
        var rpcCall = new BitcoindSignMessageRpcCall(request);
        String signature = rpcClient.invokeAndValidate(rpcCall);

        walletLock();
        return signature;
    }

    public String unblindRawTransaction(String rawTxInHex) {
        var request = new ElementsdUnblindRawTransactionRpcCall.Request(rawTxInHex);
        var rpcCall = new ElementsdUnblindRawTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall).getHex();
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

    public void walletLock() {
        var rpcCall = new BitcoindWalletLockRpcCall();
        rpcClient.invokeAndValidate(rpcCall);
    }

    public void walletPassphrase(Optional<String> passphrase, long timeout) {
        String passphraseString = passphrase.orElse("");
        if (passphraseString.isEmpty()) {
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
