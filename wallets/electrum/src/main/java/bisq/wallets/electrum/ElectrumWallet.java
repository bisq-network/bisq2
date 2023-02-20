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

package bisq.wallets.electrum;

import bisq.common.observable.ObservableSet;
import bisq.wallets.core.Wallet;
import bisq.wallets.core.model.*;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.responses.ElectrumDeserializeResponse;
import bisq.wallets.electrum.rpc.responses.ElectrumOnChainHistoryResponse;
import bisq.wallets.electrum.rpc.responses.ElectrumOnChainTransactionResponse;
import bisq.wallets.json_rpc.JsonRpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ElectrumWallet implements Wallet {
    private final Path walletPath;
    private final ElectrumDaemon daemon;

    public ElectrumWallet(Path walletPath, ElectrumDaemon daemon, ObservableSet<String> receiveAddresses) {
        this.walletPath = walletPath;
        this.daemon = daemon;
    }

    @Override
    public void initialize(Optional<String> walletPassphrase) {
        if (!doesWalletExist(walletPath)) {
            daemon.create(walletPassphrase);
        }

        daemon.loadWallet(walletPassphrase);
    }

    @Override
    public void shutdown() {
        daemon.stop();
        // Electrum does not provide an unload wallet rpc call.
    }

    @Override
    public double getBalance() {
        return daemon.getBalance();
    }

    @Override
    public String getUnusedAddress() {
        return daemon.getUnusedAddress();
    }

    @Override
    public List<? extends TransactionInfo> listTransactions() {
        ElectrumOnChainHistoryResponse onChainHistoryResponse = daemon.onChainHistory();
        return onChainHistoryResponse.getResult().getTransactions();
    }

    @Override
    public List<? extends Utxo> listUnspent() {
        return daemon.listUnspent()
                .stream()
                .map(JsonRpcResponse::getResult)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public String sendToAddress(Optional<String> passphrase, String address, double amount) {
        String unsignedTx = daemon.payTo(passphrase, address, amount);
        String signedTx = daemon.signTransaction(passphrase, unsignedTx);
        return daemon.broadcast(signedTx);
    }

    @Override
    public String signMessage(Optional<String> passphrase, String address, String message) {
        return daemon.signMessage(passphrase, address, message);
    }

    @Override
    public List<String> getWalletAddresses() {
        return daemon.listAddresses();
    }


    public ElectrumDeserializeResponse.Result getElectrumTransaction(String txId) {
        String txAsHex = daemon.getTransaction(txId);
        return daemon.deserialize(txAsHex).getResult();
    }

    @Override
    public List<Transaction> getTransactions() {
        ElectrumOnChainHistoryResponse onChainHistoryResponse = daemon.onChainHistory();
        List<ElectrumOnChainTransactionResponse> responses = onChainHistoryResponse.getResult().getTransactions();
        return responses.stream().map(response -> {
            String txId = response.getTxId();
            ElectrumDeserializeResponse.Result deserializedTx = getElectrumTransaction(txId);
            List<TransactionInput> inputs = deserializedTx.getInputs().stream()
                    .map(inputResponse -> new TransactionInput(inputResponse.getPrevOutHash(), inputResponse.getPrevOutN(), inputResponse.getNSequence(), inputResponse.getScriptSig(), inputResponse.getWitness()))
                    .collect(Collectors.toList());
            List<TransactionOutput> outputs = deserializedTx.getOutputs().stream()
                    .map(outputResponse -> new TransactionOutput(outputResponse.getValueSats(), outputResponse.getAddress(), outputResponse.getScriptPubKey()))
                    .collect(Collectors.toList());
            return new Transaction(txId,
                    inputs,
                    outputs,
                    deserializedTx.getLockTime(),
                    response.getHeight(),
                    response.getDate(),
                    response.getConfirmations(),
                    response.getAmount(),
                    response.isIncoming()
            );
        }).collect(Collectors.toList());
    }

    void notify(String address, String endpointUrl) {
        daemon.notify(address, endpointUrl);
    }

    private boolean doesWalletExist(Path walletPath) {
        return walletPath.toFile().exists();
    }
}
