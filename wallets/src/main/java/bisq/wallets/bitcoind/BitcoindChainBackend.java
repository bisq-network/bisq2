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

import bisq.wallets.bitcoind.responses.CreateOrLoadWalletResponse;
import bisq.wallets.bitcoind.responses.FinalizePsbtResponse;
import bisq.wallets.bitcoind.responses.UnloadWalletResponse;
import bisq.wallets.bitcoind.rpc.RpcClient;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class BitcoindChainBackend {

    private final RpcClient rpcClient;

    public BitcoindChainBackend(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public void createOrLoadWallet(Path walletPath, String passphrase, boolean isWatchOnly, boolean createBlankWallet) {
        if (!doesWalletExist(walletPath)) {
            createWallet(walletPath, passphrase, isWatchOnly, createBlankWallet);
        } else {
            List<String> loadedWallets = listWallets();
            if (!loadedWallets.contains(walletPath.toString())) {
                loadWallet(walletPath);
            }
        }
    }

    public FinalizePsbtResponse finalizePsbt(String psbt) {
        return rpcClient.invoke(
                BitcoindRpcEndpoint.FINALIZE_PSBT,
                new Object[]{psbt},
                FinalizePsbtResponse.class
        );
    }

    public List<String> generateToAddress(int numberOfBlocksToMine, String addressOfMiner) {
        return Arrays.asList(
                rpcClient.invoke(
                        BitcoindRpcEndpoint.GENERATE_TO_ADDRESS,
                        new Object[]{numberOfBlocksToMine, addressOfMiner},
                        String[].class
                )
        );
    }

    public List<String> listWallets() {
        return Arrays.asList(
                rpcClient.invoke(
                        BitcoindRpcEndpoint.LIST_WALLETS,
                        new Object[0],
                        String[].class
                )
        );
    }

    public String sendRawTransaction(String hexString) {
        return rpcClient.invoke(
                BitcoindRpcEndpoint.SEND_RAW_TRANSACTION,
                new Object[]{hexString},
                String.class
        );
    }

    public boolean stop() {
        String result = rpcClient.invoke(
                BitcoindRpcEndpoint.STOP,
                new Object[0],
                String.class
        );
        return result.equals("Bitcoin Core stopping");
    }

    public void unloadWallet(Path walletPath) {
        String absoluteWalletPath = walletPath.toAbsolutePath().toString();
        UnloadWalletResponse response = rpcClient.invoke(
                BitcoindRpcEndpoint.UNLOAD_WALLET,
                new Object[]{absoluteWalletPath},
                UnloadWalletResponse.class
        );
        response.validate();
    }

    private boolean doesWalletExist(Path walletPath) {
        return walletPath.toFile().exists();
    }

    private void createWallet(Path walletPath, String passphrase, boolean isWatchOnly, boolean createBlankWallet) {
        String absoluteWalletPath = walletPath.toAbsolutePath().toString();
        CreateOrLoadWalletResponse response = rpcClient.invoke(
                BitcoindRpcEndpoint.CREATE_WALLET,
                new Object[]{
                        absoluteWalletPath, // filename
                        isWatchOnly,
                        createBlankWallet,
                        passphrase
                },
                CreateOrLoadWalletResponse.class
        );
        response.validate(absoluteWalletPath);
    }

    private void loadWallet(Path walletPath) {
        String absoluteWalletPath = walletPath.toAbsolutePath().toString();
        CreateOrLoadWalletResponse response = rpcClient.invoke(
                BitcoindRpcEndpoint.LOAD_WALLET,
                new Object[]{absoluteWalletPath},
                CreateOrLoadWalletResponse.class
        );
        response.validate(absoluteWalletPath);
    }
}
