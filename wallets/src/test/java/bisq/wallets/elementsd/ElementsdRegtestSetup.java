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

package bisq.wallets.elementsd;

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.wallets.AbstractRegtestSetup;
import bisq.wallets.AddressType;
import bisq.wallets.NetworkType;
import bisq.wallets.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.elementsd.process.ElementsdProcess;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdListUnspentResponseEntry;
import bisq.wallets.process.MultiProcessCoordinator;
import bisq.wallets.rpc.RpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;
import lombok.Getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;

public class ElementsdRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator, ElementsdWallet> {

    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup;
    private final ElementsdProcess elementsdProcess;

    private ElementsdConfig elementsdConfig;

    @Getter
    private final ElementsdDaemon daemon;
    private final Set<Path> loadedWalletPaths;

    @Getter
    private ElementsdWallet minerWallet;

    public ElementsdRegtestSetup() throws IOException {
        super();
        bitcoindRegtestSetup = new BitcoindRegtestSetup();
        elementsdProcess = createElementsdProcess();

        daemon = createDaemon();
        loadedWalletPaths = new HashSet<>();
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindRegtestSetup, elementsdProcess)
        );
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

    @Override
    public void mineOneBlock() {
        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        daemon.generateToAddress(1, minerAddress);
    }

    @Override
    public void fundWallet(ElementsdWallet receiverWallet, double amount) {
        sendBtcAndMineOneBlock(minerWallet, receiverWallet, amount);
    }

    @Override
    public RpcConfig getRpcConfig() {
        return elementsdConfig.elementsdRpcConfig();
    }

    public String sendBtcAndMineOneBlock(ElementsdWallet senderWallet,
                                         ElementsdWallet receiverWallet,
                                         double amount) {
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        String txId = senderWallet.sendLBtcToAddress(receiverAddress, amount);
        mineOneBlock();
        return txId;
    }


    public ElementsdWallet createNewWallet(String walletName) throws MalformedURLException {
        Path receiverWalletPath = tmpDirPath.resolve(walletName);
        return createNewWallet(receiverWalletPath);
    }

    @Override
    public ElementsdWallet createNewWallet(Path walletPath) throws MalformedURLException {
        if (loadedWalletPaths.contains(walletPath)) {
            throw new IllegalStateException("Cannot create wallet '" + walletPath.toAbsolutePath() +
                    "'. It exists already.");
        }

        daemon.createOrLoadWallet(walletPath, Optional.of(WALLET_PASSPHRASE));
        loadedWalletPaths.add(walletPath);

        ElementsdWallet walletBackend = newWallet(walletPath);
        walletBackend.walletPassphrase(Optional.of(WALLET_PASSPHRASE), BitcoindWallet.DEFAULT_WALLET_TIMEOUT);
        return walletBackend;
    }

    public Optional<ElementsdListUnspentResponseEntry> filterUtxosByTxId(
            List<ElementsdListUnspentResponseEntry> utxos,
            String txId) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getTxId(), txId))
                .findFirst();
    }

    private ElementsdProcess createElementsdProcess() throws IOException {
        Path elementsdDataDir = tmpDirPath.resolve("elementsd");
        elementsdConfig = createElementsRpcConfig();
        return new ElementsdProcess(elementsdConfig, elementsdDataDir);
    }

    private ElementsdDaemon createDaemon() throws MalformedURLException {
        RpcClient rpcClient = RpcClientFactory.create(elementsdConfig.elementsdRpcConfig());
        return new ElementsdDaemon(rpcClient);
    }

    private ElementsdWallet newWallet(Path walletPath) throws MalformedURLException {
        RpcConfig walletRpcConfig = new RpcConfig.Builder(elementsdConfig.elementsdRpcConfig())
                .walletPath(walletPath)
                .build();
        RpcClient rpcClient = RpcClientFactory.create(walletRpcConfig);
        return new ElementsdWallet(rpcClient);
    }

    private ElementsdConfig createElementsRpcConfig() throws IOException {
        int elementsPort = NetworkUtils.findFreeSystemPort();
        RpcConfig elementsdConfig = createRpcConfigForPort(elementsPort);

        RpcConfig bitcoindConfig = bitcoindRegtestSetup.getRpcConfig();
        return new ElementsdConfig(bitcoindConfig, elementsdConfig);
    }

    private RpcConfig createRpcConfigForPort(int port) throws IOException {
        Path walletPath = FileUtils.createTempDir();
        return new RpcConfig.Builder()
                .networkType(NetworkType.REGTEST)
                .hostname("127.0.0.1")
                .user("bisq")
                .password("bisq")
                .port(port)
                .walletPath(walletPath)
                .build();
    }
}
