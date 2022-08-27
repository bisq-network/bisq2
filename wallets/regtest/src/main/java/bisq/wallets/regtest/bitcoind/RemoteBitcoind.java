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

package bisq.wallets.regtest.bitcoind;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.zmq.BitcoindRawTxProcessor;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.bitcoind.zmq.ZmqTopicProcessors;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.process.BisqProcess;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static bisq.wallets.regtest.AbstractRegtestSetup.WALLET_PASSPHRASE;

public class RemoteBitcoind implements BisqProcess {

    private final Path tmpDirPath;
    private final RpcConfig rpcConfig;
    @Getter
    private final BitcoindDaemon daemon;
    private final boolean doMineInitialRegtestBlocks;
    @Getter
    private final ZmqListeners zmqListeners = new ZmqListeners();
    @Getter
    private final BitcoindWallet minerWallet;
    private final BitcoindRegtestBlockMiner blockMiner;
    private final List<BitcoindWallet> loadedWallets = new ArrayList<>();
    private ZmqConnection bitcoindZeroMq;

    public RemoteBitcoind(Path tmpDirPath,
                          RpcConfig rpcConfig,
                          boolean doMineInitialRegtestBlocks) throws MalformedURLException {
        this.tmpDirPath = tmpDirPath;
        this.rpcConfig = rpcConfig;
        this.daemon = createBitcoindDaemon();
        this.doMineInitialRegtestBlocks = doMineInitialRegtestBlocks;
        this.minerWallet = createNewWallet("miner_wallet");
        this.blockMiner = new BitcoindRegtestBlockMiner(daemon, minerWallet, zmqListeners);
    }

    @Override
    public void start() throws InterruptedException, IOException {
        blockMiner.start();
        initializeZmqListeners();
        initializeWallet(minerWallet);

        if (doMineInitialRegtestBlocks) {
            mineInitialRegtestBlocks();
        }
    }

    @Override
    public void shutdown() {
        blockMiner.shutdown();
        bitcoindZeroMq.close();
        loadedWallets.forEach(BitcoindWallet::shutdown);
    }

    public BitcoindWallet createAndInitializeNewWallet(String walletName) throws MalformedURLException {
        var bitcoindWallet = createNewWallet(walletName);
        bitcoindWallet.initialize(Optional.of(WALLET_PASSPHRASE));
        return bitcoindWallet;
    }

    public void mineInitialRegtestBlocks() throws InterruptedException {
        blockMiner.mineInitialRegtestBlocks();
    }

    public List<String> mineBlocks(int numberOfBlocks) throws InterruptedException {
        return blockMiner.mineBlocks(numberOfBlocks);
    }

    public void fundWallet(BitcoindWallet receiverWallet, double amount) throws InterruptedException {
        sendBtcAndMineOneBlock(minerWallet, receiverWallet, amount);
    }

    public String fundAddress(String address, double amount) throws InterruptedException {
        String txId = minerWallet.sendToAddress(Optional.of(WALLET_PASSPHRASE), address, amount);
        mineOneBlock();
        return txId;
    }

    public String sendBtcAndMineOneBlock(BitcoindWallet senderWallet,
                                         BitcoindWallet receiverWallet,
                                         double amount) throws InterruptedException {
        String receiverAddress = receiverWallet.getNewAddress(AddressType.BECH32, "");
        senderWallet.sendToAddress(Optional.of(WALLET_PASSPHRASE), receiverAddress, amount);
        mineOneBlock();
        return receiverAddress;
    }

    public List<String> mineOneBlock() throws InterruptedException {
        return mineBlocks(1);
    }

    public CountDownLatch waitUntilBlocksMined(List<String> blockHashes) {
        return blockMiner.waitUntilBlocksMined(blockHashes);
    }

    private BitcoindDaemon createBitcoindDaemon() {
        DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new BitcoindDaemon(rpcClient);
    }

    private BitcoindWallet createNewWallet(String walletName) throws MalformedURLException {
        Path walletPath = tmpDirPath.resolve(walletName);
        checkWhetherWalletExist(walletPath);
        return new BitcoindWallet(daemon, rpcConfig, walletPath);
    }

    private void checkWhetherWalletExist(Path walletPath) {
        File walletFile = walletPath.toFile();
        if (walletFile.exists()) {
            throw new IllegalStateException("Cannot create wallet '" + walletPath.toAbsolutePath() +
                    "'. It exists already.");
        }
    }

    private void initializeZmqListeners() {
        var bitcoindRawTxProcessor = new BitcoindRawTxProcessor(daemon, zmqListeners);
        var bitcoindZmqTopicProcessors = new ZmqTopicProcessors(bitcoindRawTxProcessor, zmqListeners);
        bitcoindZeroMq = new ZmqConnection(bitcoindZmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = daemon.getZmqNotifications();
        bitcoindZeroMq.initialize(zmqNotifications);
    }

    private void initializeWallet(BitcoindWallet wallet) {
        wallet.initialize(Optional.of(WALLET_PASSPHRASE));
        loadedWallets.add(wallet);
    }
}
