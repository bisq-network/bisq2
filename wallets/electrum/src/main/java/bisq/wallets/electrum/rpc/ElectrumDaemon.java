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

package bisq.wallets.electrum.rpc;

import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.electrum.rpc.calls.*;
import bisq.wallets.electrum.rpc.responses.*;

import java.util.Arrays;
import java.util.List;

public class ElectrumDaemon {
    private final DaemonRpcClient rpcClient;

    public ElectrumDaemon(DaemonRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public String broadcast(String tx) {
        var request = new ElectrumBroadcastRpcCall.Request(tx);
        var rpcCall = new ElectrumBroadcastRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public ElectrumCreateResponse create(String password) {
        var request = new ElectrumCreateRpcCall.Request(password);
        var rpcCall = new ElectrumCreateRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public ElectrumDeserializeResponse deserialize(String tx) {
        var request = new ElectrumDeserializeRpcCall.Request(tx);
        var rpcCall = new ElectrumDeserializeRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public double getBalance() {
        var rpcCall = new ElectrumGetBalanceRpcCall();
        ElectrumGetBalanceResponse response = rpcClient.invokeAndValidate(rpcCall);
        double confirmedBalance = Double.parseDouble(response.getConfirmed());

        String unconfirmed = response.getUnconfirmed();
        double unconfirmedBalance = unconfirmed != null ? Double.parseDouble(response.getUnconfirmed()) : 0;

        return confirmedBalance + unconfirmedBalance;
    }

    public ElectrumGetInfoResponse getInfo() {
        var rpcCall = new ElectrumGetInfoRpcCall();
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String getSeed(String password) {
        var request = new ElectrumGetSeedRpcCall.Request(password);
        var rpcCall = new ElectrumGetSeedRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String getTransaction(String txId) {
        var request = new ElectrumGetTransactionRpcCall.Request(txId);
        var rpcCall = new ElectrumGetTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String getUnusedAddress() {
        var rpcCall = new ElectrumGetUnusedAddressRpcCall();
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public List<ElectrumListUnspentResponseEntry> listUnspent() {
        var rpcCall = new ElectrumListUnspentRpcCall();
        return Arrays.asList(rpcClient.invokeAndValidate(rpcCall));
    }

    public void loadWallet(String password) {
        var request = new ElectrumLoadWalletRpcCall.Request(password);
        var rpcCall = new ElectrumLoadWalletRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public void notify(String bitcoinAddress, String url) {
        var request = new ElectrumNotifyRpcCall.Request(bitcoinAddress, url);
        var rpcCall = new ElectrumNotifyRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public ElectrumOnChainHistoryResponse onChainHistory() {
        var rpcCall = new ElectrumOnChainHistoryRpcCall();
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public void password(String password, String newPassword) {
        var request = ElectrumPasswordRpcCall.Request.builder()
                .password(password)
                .newPassword(newPassword)
                .build();
        var rpcCall = new ElectrumPasswordRpcCall(request);
        rpcClient.invokeAndValidate(rpcCall);
    }

    public String payTo(String destination, double amount, String password) {
        var request = ElectrumPayToRpcCall.Request.builder()
                .destination(destination)
                .amount(amount)
                .password(password)
                .build();
        var rpcCall = new ElectrumPayToRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String signMessage(String password, String address, String message) {
        var request = ElectrumSignMessageRpcCall.Request.builder()
                .password(password)
                .address(address)
                .message(message)
                .build();
        var rpcCall = new ElectrumSignMessageRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public String signTransaction(String tx, String password) {
        var request = new ElectrumSignTransactionRpcCall.Request(tx, password);
        var rpcCall = new ElectrumSignTransactionRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall);
    }

    public void stop() {
        var rpcCall = new ElectrumStopRpcCall();
        rpcClient.invokeAndValidate(rpcCall);
    }

    public boolean verifyMessage(String address, String signature, String message) {
        var request = ElectrumVerifyMessageRpcCall.Request.builder()
                .address(address)
                .signature(signature)
                .message(message)
                .build();
        var rpcCall = new ElectrumVerifyMessageRpcCall(request);
        return rpcClient.invokeAndValidate(rpcCall).equals("true");
    }
}
