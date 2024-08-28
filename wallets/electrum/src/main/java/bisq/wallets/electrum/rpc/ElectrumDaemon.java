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

import bisq.wallets.electrum.rpc.calls.*;
import bisq.wallets.electrum.rpc.responses.*;
import bisq.wallets.json_rpc.JsonRpcClient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ElectrumDaemon {
    private final JsonRpcClient rpcClient;

    public ElectrumDaemon(JsonRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public String broadcast(String tx) {
        var request = new ElectrumBroadcastRpcCall.Request(tx);
        var rpcCall = new ElectrumBroadcastRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public ElectrumCreateResponse create(Optional<String> password) {
        var request = new ElectrumCreateRpcCall.Request(password.orElse(null));
        var rpcCall = new ElectrumCreateRpcCall(request);
        return rpcClient.call(rpcCall);
    }

    public ElectrumDeserializeResponse deserialize(String tx) {
        var request = new ElectrumDeserializeRpcCall.Request(tx);
        var rpcCall = new ElectrumDeserializeRpcCall(request);
        return rpcClient.call(rpcCall);
    }

    public double getBalance() {
        var rpcCall = new ElectrumGetBalanceRpcCall();
        ElectrumGetBalanceResponse.Result response = rpcClient.call(rpcCall).getResult();
        double confirmedBalance = Double.parseDouble(response.getConfirmed());

        String unconfirmed = response.getUnconfirmed();
        double unconfirmedBalance = unconfirmed != null ? Double.parseDouble(response.getUnconfirmed()) : 0;

        return confirmedBalance + unconfirmedBalance;
    }

    public ElectrumGetInfoResponse getInfo() {
        var rpcCall = new ElectrumGetInfoRpcCall();
        return rpcClient.call(rpcCall);
    }

    public String getSeed(Optional<String> password) {
        var request = new ElectrumGetSeedRpcCall.Request(password.orElse(null));
        var rpcCall = new ElectrumGetSeedRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public String getTransaction(String txId) {
        var request = new ElectrumGetTransactionRpcCall.Request(txId);
        var rpcCall = new ElectrumGetTransactionRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public String getUnusedAddress() {
        var rpcCall = new ElectrumGetUnusedAddressRpcCall();
        return rpcClient.call(rpcCall).getResult();
    }

    public List<ElectrumListUnspentResponse> listUnspent() {
        var rpcCall = new ElectrumListUnspentRpcCall();
        return Arrays.asList(rpcClient.call(rpcCall));
    }

    public void loadWallet(Optional<String> password) {
        var request = new ElectrumLoadWalletRpcCall.Request(password.orElse(null));
        var rpcCall = new ElectrumLoadWalletRpcCall(request);
        rpcClient.call(rpcCall);
    }

    public void notify(String bitcoinAddress, String url) {
        var request = new ElectrumNotifyRpcCall.Request(bitcoinAddress, url);
        var rpcCall = new ElectrumNotifyRpcCall(request);
        rpcClient.call(rpcCall);
    }

    public ElectrumOnChainHistoryResponse onChainHistory() {
        var rpcCall = new ElectrumOnChainHistoryRpcCall();
        return rpcClient.call(rpcCall);
    }

    public void password(Optional<String> password, String newPassword) {
        var request = ElectrumPasswordRpcCall.Request.builder()
                .password(password.orElse(null))
                .newPassword(newPassword)
                .build();
        var rpcCall = new ElectrumPasswordRpcCall(request);
        rpcClient.call(rpcCall);
    }

    public String payTo(Optional<String> password, String destination, double amount) {
        var request = ElectrumPayToRpcCall.Request.builder()
                .destination(destination)
                .amount(amount)
                .password(password.orElse(null))
                .build();
        var rpcCall = new ElectrumPayToRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public String signMessage(Optional<String> password, String address, String message) {
        var request = ElectrumSignMessageRpcCall.Request.builder()
                .password(password.orElse(null))
                .address(address)
                .message(message)
                .build();
        var rpcCall = new ElectrumSignMessageRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public String signTransaction(Optional<String> password, String tx) {
        var request = new ElectrumSignTransactionRpcCall.Request(tx, password.orElse(null));
        var rpcCall = new ElectrumSignTransactionRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public void stop() {
        var rpcCall = new ElectrumStopRpcCall();
        rpcClient.call(rpcCall);
    }

    public boolean verifyMessage(String address, String signature, String message) {
        var request = ElectrumVerifyMessageRpcCall.Request.builder()
                .address(address)
                .signature(signature)
                .message(message)
                .build();
        var rpcCall = new ElectrumVerifyMessageRpcCall(request);
        return rpcClient.call(rpcCall).getResult();
    }

    public List<String> listAddresses() {
        var rpcCall = new ElectrumListAddressesRpcCall();
        ElectrumListAddressesResponse call = rpcClient.call(rpcCall);
        return call.getResult();
    }
}
