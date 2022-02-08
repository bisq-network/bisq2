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

package bisq.wallets.bitcoind;

import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.psbt.PsbtInput;
import bisq.wallets.bitcoind.psbt.PsbtOptions;
import bisq.wallets.bitcoind.psbt.PsbtOutput;
import bisq.wallets.bitcoind.responses.*;
import bisq.wallets.bitcoind.rpc.RpcClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BitcoindWalletBackend {

    public static final long DEFAULT_WALLET_TIMEOUT = TimeUnit.HOURS.toSeconds(24);
    private final RpcClient walletRpcClient;

    public BitcoindWalletBackend(RpcClient walletRpcClient) {
        this.walletRpcClient = walletRpcClient;
    }

    public AddMultisigAddressResponse addMultisigAddress(int nRequired, List<String> keys) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.ADD_MULTISIG_ADDRESS,
                new Object[]{nRequired, keys},
                AddMultisigAddressResponse.class
        );
    }

    public GetAddressInfoResponse getAddressInfo(String address) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.GET_ADDRESS_INFO,
                new Object[]{address},
                GetAddressInfoResponse.class
        );
    }

    public double getBalance() {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.GET_BALANCE,
                new Object[0],
                Double.class
        );
    }

    public String getNewAddress(AddressType addressType, String label) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.GET_NEW_ADDRESS,
                new Object[]{label, addressType.getName()},
                String.class
        );
    }

    public void importAddress(String address, String label) {
        walletRpcClient.invoke(
                BitcoindRpcEndpoint.IMPORT_ADDRESS,
                new Object[]{address, label},
                null
        );
    }

    public List<ListTransactionsResponseEntry> listTransactions(int count) {
        return Arrays.asList(
                walletRpcClient.invoke(
                        BitcoindRpcEndpoint.LIST_TRANSACTIONS,
                        new Object[]{"*", count},
                        ListTransactionsResponseEntry[].class
                )
        );
    }

    public List<ListUnspentResponseEntry> listUnspent() {
        return Arrays.asList(
                walletRpcClient.invoke(
                        BitcoindRpcEndpoint.LIST_UNSPENT,
                        new Object[0],
                        ListUnspentResponseEntry[].class
                )
        );
    }

    public String sendToAddress(String address, double amount) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.SEND_TO_ADDRESS,
                new Object[]{address, amount},
                String.class
        );
    }

    public String signMessage(String address, String message) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.SIGN_MESSAGE,
                new Object[]{address, message},
                String.class
        );
    }

    public boolean verifyMessage(String address, String signature, String message) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.VERIFY_MESSAGE,
                new Object[]{address, signature, message},
                Boolean.class
        );
    }

    public WalletCreateFundedPsbtResponse walletCreateFundedPsbt(List<PsbtInput> inputs,
                                                                 PsbtOutput psbtOutput,
                                                                 int lockTime,
                                                                 PsbtOptions psbtOptions) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.WALLET_CREATE_FUNDED_PSBT,
                new Object[]{
                        inputs,
                        psbtOutput.toPsbtOutputObject(),
                        lockTime,
                        psbtOptions
                },
                WalletCreateFundedPsbtResponse.class
        );
    }

    public void walletPassphrase(String passphrase, long timeout) {
        walletRpcClient.invoke(
                BitcoindRpcEndpoint.WALLET_PASSPHRASE,
                new Object[]{
                        passphrase,
                        timeout
                },
                null
        );
    }

    public WalletProcessPsbtResponse walletProcessPsbt(String psbt) {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.WALLET_PROCESS_PSBT,
                new Object[]{psbt},
                WalletProcessPsbtResponse.class
        );
    }
}
