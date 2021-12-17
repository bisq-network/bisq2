package network.misq.wallets.bitcoind;

import network.misq.wallets.AddressType;
import network.misq.wallets.bitcoind.psbt.PsbtInput;
import network.misq.wallets.bitcoind.psbt.PsbtOptions;
import network.misq.wallets.bitcoind.psbt.PsbtOutput;
import network.misq.wallets.bitcoind.responses.*;
import network.misq.wallets.bitcoind.rpc.RpcCallFailureException;
import network.misq.wallets.bitcoind.rpc.RpcClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BitcoindWalletBackend {

    public static final long DEFAULT_WALLET_TIMEOUT = TimeUnit.HOURS.toSeconds(24);
    private final RpcClient walletRpcClient;

    public BitcoindWalletBackend(RpcClient walletRpcClient) {
        this.walletRpcClient = walletRpcClient;
    }

    public AddMultisigAddressResponse addMultisigAddress(int nRequired, List<String> keys) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.ADD_MULTISIG_ADDRESS,
                new Object[]{nRequired, keys},
                AddMultisigAddressResponse.class
        );
    }

    public GetAddressInfoResponse getAddressInfo(String address) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.GET_ADDRESS_INFO,
                new Object[]{address},
                GetAddressInfoResponse.class
        );
    }

    public double getBalance() throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.GET_BALANCE,
                new Object[0],
                Double.class
        );
    }

    public String getNewAddress(AddressType addressType, String label) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.GET_NEW_ADDRESS,
                new Object[]{label, addressType.getName()},
                String.class
        );
    }

    public void importAddress(String address, String label) throws RpcCallFailureException {
        walletRpcClient.invoke(
                BitcoindRpcEndpoint.IMPORT_ADDRESS,
                new Object[]{address, label},
                null
        );
    }

    public List<ListTransactionsResponseEntry> listTransactions(int count) throws RpcCallFailureException {
        return Arrays.asList(
                walletRpcClient.invoke(
                        BitcoindRpcEndpoint.LIST_TRANSACTIONS,
                        new Object[]{"*", count},
                        ListTransactionsResponseEntry[].class
                )
        );
    }

    public List<ListUnspentResponseEntry> listUnspent() throws RpcCallFailureException {
        return Arrays.asList(
                walletRpcClient.invoke(
                        BitcoindRpcEndpoint.LIST_UNSPENT,
                        new Object[0],
                        ListUnspentResponseEntry[].class
                )
        );
    }

    public String sendToAddress(String address, double amount) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.SEND_TO_ADDRESS,
                new Object[]{address, amount},
                String.class
        );
    }

    public String signMessage(String address, String message) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.SIGN_MESSAGE,
                new Object[]{address, message},
                String.class
        );
    }

    public boolean verifyMessage(String address, String signature, String message) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.VERIFY_MESSAGE,
                new Object[]{address, signature, message},
                Boolean.class
        );
    }

    public WalletCreateFundedPsbtResponse walletCreateFundedPsbt(List<PsbtInput> inputs,
                                                                 PsbtOutput psbtOutput,
                                                                 int lockTime,
                                                                 PsbtOptions psbtOptions) throws RpcCallFailureException {
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

    public void walletPassphrase(String passphrase, long timeout) throws RpcCallFailureException {
        walletRpcClient.invoke(
                BitcoindRpcEndpoint.WALLET_PASSPHRASE,
                new Object[]{
                        passphrase,
                        timeout
                },
                null
        );
    }

    public WalletProcessPsbtResponse walletProcessPsbt(String psbt) throws RpcCallFailureException {
        return walletRpcClient.invoke(
                BitcoindRpcEndpoint.WALLET_PROCESS_PSBT,
                new Object[]{psbt},
                WalletProcessPsbtResponse.class
        );
    }
}
