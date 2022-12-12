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

package bisq.wallets.elementsd.regtest;

import bisq.common.data.Pair;
import bisq.common.util.NetworkUtils;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.bitcoind.zmq.ZmqTopicProcessors;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.elementsd.ElementsdConfig;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdRawTxProcessor;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdListUnspentResponse;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ElementsdRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator> {

    @Getter
    private final BitcoindRegtestSetup bitcoindRegtestSetup;
    private final ElementsdRegtestProcess elementsdProcess;

    private ElementsdConfig elementsdConfig;

    @Getter
    private final ElementsdDaemon daemon;
    private final Set<String> loadedWalletPaths;

    @Getter
    private ZmqListeners zmqMinerListeners;
    private ZmqConnection zmqMinerWalletConnection;
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
    public void start() throws InterruptedException {
        super.start();
        minerWallet = createNewWallet("miner_wallet");

        Pair<ZmqConnection, ZmqListeners> connectionAndListeners = initializeZmqListenersForWallet(minerWallet);
        zmqMinerWalletConnection = connectionAndListeners.getFirst();
        zmqMinerListeners = connectionAndListeners.getSecond();
    }

    @Override
    public void shutdown() {
        zmqMinerWalletConnection.close();
        loadedWalletPaths.forEach(daemon::unloadWallet);
        super.shutdown();
    }

    @Override
    public List<String> mineOneBlock() throws InterruptedException {
        CountDownLatch blockMinedLatch = waitUntilBlockMined();

        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        List<String> blockHashes = daemon.generateToAddress(1, minerAddress);

        boolean allBlocksMined = blockMinedLatch.await(15, TimeUnit.SECONDS);
        if (!allBlocksMined) {
            throw new IllegalStateException("Couldn't mine block.");
        }

        return blockHashes;
    }

    @Override
    public RpcConfig getRpcConfig() {
        return elementsdConfig.elementsdRpcConfig();
    }

    public String sendBtcAndMineOneBlock(ElementsdWallet senderWallet,
                                         ElementsdWallet receiverWallet,
                                         double amount) throws InterruptedException {
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        String txId = senderWallet.sendLBtcToAddress(Optional.of(ElementsdRegtestSetup.WALLET_PASSPHRASE), receiverAddress, amount);
        mineOneBlock();
        return txId;
    }

    public ElementsdWallet createNewWallet(String walletName) {
        if (loadedWalletPaths.contains(walletName)) {
            throw new IllegalStateException("Cannot create wallet '" + walletName +
                    "'. It exists already.");
        }

        daemon.createOrLoadWallet(walletName, Optional.of(WALLET_PASSPHRASE));
        loadedWalletPaths.add(walletName);

        return newWallet(walletName);
    }

    public Optional<ElementsdListUnspentResponse.Entry> filterUtxosByTxId(
            List<ElementsdListUnspentResponse.Entry> utxos,
            String txId) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getTxId(), txId))
                .findFirst();
    }

    public Pair<ZmqConnection, ZmqListeners> initializeZmqListenersForWallet(ElementsdWallet wallet) {
        var zmqListeners = new ZmqListeners();
        var rawTxProcessor = new ElementsdRawTxProcessor(daemon, wallet, zmqListeners);
        var zmqTopicProcessors = new ZmqTopicProcessors(rawTxProcessor, zmqListeners);
        var zmqConnection = new ZmqConnection(zmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse.Entry> zmqNotifications = daemon.getZmqNotifications();
        zmqConnection.initialize(zmqNotifications);

        return new Pair<>(zmqConnection, zmqListeners);
    }

    private ElementsdRegtestProcess createElementsdProcess() {
        Path elementsdDataDir = tmpDirPath.resolve("elementsd");
        elementsdConfig = createElementsRpcConfig();
        return new ElementsdRegtestProcess(elementsdConfig, elementsdDataDir);
    }

    private ElementsdDaemon createDaemon() {
        JsonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(elementsdConfig.elementsdRpcConfig());
        return new ElementsdDaemon(rpcClient);
    }

    private ElementsdWallet newWallet(String walletName) {
        RpcConfig walletRpcConfig = elementsdConfig.elementsdRpcConfig();
        JsonRpcClient rpcClient = RpcClientFactory.createWalletRpcClient(walletRpcConfig, walletName);
        return new ElementsdWallet(rpcClient);
    }

    private ElementsdConfig createElementsRpcConfig() {
        int elementsPort = NetworkUtils.findFreeSystemPort();
        RpcConfig elementsdConfig = createRpcConfigForPort(elementsPort);

        RpcConfig bitcoindConfig = bitcoindRegtestSetup.getRpcConfig();
        return new ElementsdConfig(bitcoindConfig, elementsdConfig);
    }

    private CountDownLatch waitUntilBlockMined() {
        var blockMinedLatch = new CountDownLatch(1);
        zmqMinerListeners.registerNewBlockMinedListener((blockHash) -> blockMinedLatch.countDown());
        return blockMinedLatch;
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
