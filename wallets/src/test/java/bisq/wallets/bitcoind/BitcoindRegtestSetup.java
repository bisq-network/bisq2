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

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.wallets.AddressType;
import bisq.wallets.NetworkType;
import bisq.wallets.bitcoind.responses.ListUnspentResponseEntry;
import bisq.wallets.bitcoind.rpc.RpcClient;
import bisq.wallets.bitcoind.rpc.RpcConfig;
import bisq.wallets.bitcoind.rpc.WalletRpcConfig;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BitcoindRegtestSetup {
    public static final String WALLET_PASSPHRASE = "My super secret passphrase that nobody can guess.";

    public static RpcConfig createRpcConfigForPort(int port) {
        return new RpcConfig.Builder()
                .networkType(NetworkType.REGTEST)
                .hostname("127.0.0.1")
                .user("bisq")
                .password("bisq")
                .port(port)
                .build();
    }

    public static BitcoindProcess createAndStartBitcoind() throws IOException {
        int freePort = NetworkUtils.findFreeSystemPort();
        RpcConfig rpcConfig = BitcoindRegtestSetup.createRpcConfigForPort(freePort);

        Path bitcoindDataDir = FileUtils.createTempDir();
        var bitcoindProcess = new BitcoindProcess(
                rpcConfig,
                bitcoindDataDir
        );
        bitcoindProcess.startAndWaitUntilReady();
        return bitcoindProcess;
    }

    public static BitcoindWalletBackend createTestWalletBackend(RpcConfig rpcConfig,
                                                                BitcoindChainBackend chainBackend,
                                                                Path tmpDirPath,
                                                                String walletName) throws MalformedURLException {
        Path receiverWalletPath = tmpDirPath.resolve(walletName);
        RpcClient receiverWalletRpc = createWalletRpcClient(rpcConfig, receiverWalletPath);

        chainBackend.createOrLoadWallet(receiverWalletPath, BitcoindRegtestSetup.WALLET_PASSPHRASE, false, false);

        var walletBackend = new BitcoindWalletBackend(receiverWalletRpc);
        walletBackend.walletPassphrase(BitcoindRegtestSetup.WALLET_PASSPHRASE, BitcoindWalletBackend.DEFAULT_WALLET_TIMEOUT);
        return walletBackend;
    }

    public static RpcClient createWalletRpcClient(RpcConfig rpcConfig, Path walletFilePath) throws MalformedURLException {
        var walletRpcConfig = new WalletRpcConfig(rpcConfig, walletFilePath);
        return new RpcClient(walletRpcConfig);
    }

    public static Optional<ListUnspentResponseEntry> filterUtxosByAddress(List<ListUnspentResponseEntry> utxos, String address) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getAddress(), address))
                .findFirst();
    }

    public static void mineInitialRegtestBlocks(BitcoindChainBackend minerChainBackend, BitcoindWalletBackend minerBackend) {
        String address = minerBackend.getNewAddress(AddressType.BECH32, "");
        minerChainBackend.generateToAddress(101, address);
    }

    public static void mineOneBlock(BitcoindChainBackend minerChainBackend, BitcoindWalletBackend minerBackend) {
        String minerAddress = minerBackend.getNewAddress(AddressType.BECH32, "");
        minerChainBackend.generateToAddress(1, minerAddress);
    }

    public static String sendBtcAndMineOneBlock(BitcoindChainBackend minerChainBackend,
                                                BitcoindWalletBackend minerWalletBackend,
                                                BitcoindWalletBackend receiverBackend,
                                                double amount) {
        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWalletBackend.sendToAddress(receiverAddress, amount);

        mineOneBlock(minerChainBackend, minerWalletBackend);
        return receiverAddress;
    }
}
