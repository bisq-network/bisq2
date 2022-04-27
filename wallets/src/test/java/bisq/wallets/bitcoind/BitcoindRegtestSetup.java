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

import bisq.common.util.NetworkUtils;
import bisq.wallets.AbstractRegtestSetup;
import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponseEntry;
import bisq.wallets.rpc.RpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;
import lombok.Getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;

public class BitcoindRegtestSetup
        extends AbstractRegtestSetup<BitcoindRegtestProcess, BitcoindWallet> {

    @Getter
    private final RpcConfig rpcConfig;
    private final BitcoindRegtestProcess bitcoindProcess;

    @Getter
    private final BitcoindDaemon daemon;
    private final Set<Path> loadedWalletPaths;

    @Getter
    private BitcoindWallet minerWallet;

    public BitcoindRegtestSetup() throws IOException {
        super();
        rpcConfig = createRpcConfig();
        bitcoindProcess = createBitcoindProcess();

        daemon = createDaemon();
        loadedWalletPaths = new HashSet<>();
    }

    @Override
    protected BitcoindRegtestProcess createProcess() {
        return bitcoindProcess;
    }

    @Override
    public void start() throws IOException {
        super.start();
        minerWallet = createNewWallet("miner_wallet");
    }

    @Override
    public void shutdown() {
        loadedWalletPaths.forEach(daemon::unloadWallet);
        super.shutdown();
    }

    public BitcoindWallet createNewWallet(String walletName) throws MalformedURLException {
        Path receiverWalletPath = tmpDirPath.resolve(walletName);
        return createNewWallet(receiverWalletPath);
    }

    @Override
    public BitcoindWallet createNewWallet(Path walletPath) throws MalformedURLException {
        if (loadedWalletPaths.contains(walletPath)) {
            throw new IllegalStateException("Cannot create wallet '" + walletPath.toAbsolutePath() +
                    "'. It exists already.");
        }

        daemon.createOrLoadWallet(walletPath, Optional.of(WALLET_PASSPHRASE));
        loadedWalletPaths.add(walletPath);

        BitcoindWallet walletBackend = newWallet(walletPath);
        walletBackend.walletPassphrase(Optional.of(WALLET_PASSPHRASE), BitcoindWallet.DEFAULT_WALLET_TIMEOUT);
        return walletBackend;
    }

    private BitcoindWallet newWallet(Path walletPath) throws MalformedURLException {
        RpcConfig walletRpcConfig = new RpcConfig.Builder(rpcConfig)
                .walletPath(walletPath)
                .build();
        RpcClient rpcClient = RpcClientFactory.create(walletRpcConfig);
        return new BitcoindWallet(rpcClient);
    }

    public void mineInitialRegtestBlocks() {
        String address = minerWallet.getNewAddress(AddressType.BECH32, "");
        daemon.generateToAddress(101, address);
    }

    @Override
    public List<String> mineOneBlock() {
        return mineBlocks(1);
    }

    public List<String> mineBlocks(int numberOfBlocks) {
        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        return daemon.generateToAddress(numberOfBlocks, minerAddress);
    }

    @Override
    public void fundWallet(BitcoindWallet receiverWallet, double amount) {
        sendBtcAndMineOneBlock(minerWallet, receiverWallet, amount);
    }

    public String fundAddress(String address, double amount) {
        String txId = minerWallet.sendToAddress(address, amount);
        mineOneBlock();
        return txId;
    }

    public String sendBtcAndMineOneBlock(BitcoindWallet senderWallet,
                                         BitcoindWallet receiverWallet,
                                         double amount) {
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        senderWallet.sendToAddress(receiverAddress, amount);
        mineOneBlock();
        return receiverAddress;
    }

    public Optional<BitcoindListUnspentResponseEntry> filterUtxosByAddress(
            List<BitcoindListUnspentResponseEntry> utxos,
            String address) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getAddress(), address))
                .findFirst();
    }

    private RpcConfig createRpcConfig() {
        int port = NetworkUtils.findFreeSystemPort();
        Path walletPath = tmpDirPath.resolve("wallet");
        return new RpcConfig.Builder()
                .hostname("127.0.0.1")
                .user("bisq")
                .password("bisq")
                .port(port)
                .walletPath(walletPath)
                .build();
    }

    private BitcoindRegtestProcess createBitcoindProcess() {
        Path bitcoindDataDir = tmpDirPath.resolve("bitcoind_data_dir");
        return new BitcoindRegtestProcess(
                rpcConfig,
                bitcoindDataDir
        );
    }

    protected BitcoindDaemon createDaemon() throws MalformedURLException {
        RpcClient rpcClient = RpcClientFactory.create(rpcConfig);
        return new BitcoindDaemon(rpcClient);
    }
}
