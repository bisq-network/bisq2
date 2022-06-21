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

import bisq.common.util.NetworkUtils;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.core.rpc.WalletRpcClient;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdListUnspentResponseEntry;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.*;

public class ElementsdRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator, ElementsdWallet> {

    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup;
    private final ElementsdRegtestProcess elementsdProcess;

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
    public void start() throws IOException, InterruptedException {
        super.start();
        minerWallet = createNewWallet("miner_wallet");
    }

    @Override
    public void shutdown() {
        loadedWalletPaths.forEach(daemon::unloadWallet);
        super.shutdown();
    }

    @Override
    public List<String> mineOneBlock() {
        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        return daemon.generateToAddress(1, minerAddress);
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
        String txId = senderWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), receiverAddress, amount);
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

        return newWallet(walletPath);
    }

    public Optional<ElementsdListUnspentResponseEntry> filterUtxosByTxId(
            List<ElementsdListUnspentResponseEntry> utxos,
            String txId) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getTxId(), txId))
                .findFirst();
    }

    private ElementsdRegtestProcess createElementsdProcess() {
        Path elementsdDataDir = tmpDirPath.resolve("elementsd");
        elementsdConfig = createElementsRpcConfig();
        return new ElementsdRegtestProcess(elementsdConfig, elementsdDataDir);
    }

    private ElementsdDaemon createDaemon() throws MalformedURLException {
        DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(elementsdConfig.elementsdRpcConfig());
        return new ElementsdDaemon(rpcClient);
    }

    private ElementsdWallet newWallet(Path walletPath) throws MalformedURLException {
        RpcConfig walletRpcConfig = elementsdConfig.elementsdRpcConfig();
        WalletRpcClient rpcClient = RpcClientFactory.createWalletRpcClient(walletRpcConfig, walletPath);
        return new ElementsdWallet(rpcClient);
    }

    private ElementsdConfig createElementsRpcConfig() {
        int elementsPort = NetworkUtils.findFreeSystemPort();
        RpcConfig elementsdConfig = createRpcConfigForPort(elementsPort);

        RpcConfig bitcoindConfig = bitcoindRegtestSetup.getRpcConfig();
        return new ElementsdConfig(bitcoindConfig, elementsdConfig);
    }

    private RpcConfig createRpcConfigForPort(int port) {
        return RpcConfig.builder()
                .hostname("127.0.0.1")
                .user("bisq")
                .password("bisq")
                .port(port)
                .build();
    }
}
