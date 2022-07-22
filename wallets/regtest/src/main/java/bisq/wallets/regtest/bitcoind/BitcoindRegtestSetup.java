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

import bisq.common.util.NetworkUtils;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import bisq.wallets.bitcoind.rpc.responses.BitcoindGetZmqNotificationsResponse;
import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponseEntry;
import bisq.wallets.bitcoind.zmq.BitcoindRawTxProcessor;
import bisq.wallets.bitcoind.zmq.ZmqConnection;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.bitcoind.zmq.ZmqTopicProcessors;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.regtest.AbstractRegtestSetup;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BitcoindRegtestSetup
        extends AbstractRegtestSetup<BitcoindRegtestProcess, BitcoindWallet> {

    private final boolean doMineInitialRegtestBlocks;
    @Getter
    private final RpcConfig rpcConfig;
    private final BitcoindRegtestProcess bitcoindProcess;

    @Getter
    private final BitcoindDaemon daemon;
    private final List<BitcoindWallet> loadedWallets = new ArrayList<>();

    @Getter
    private final ZmqListeners zmqListeners = new ZmqListeners();

    @Getter
    private final BitcoindWallet minerWallet;
    private ZmqConnection bitcoindZeroMq;

    public BitcoindRegtestSetup() throws IOException {
        this(false);
    }

    public BitcoindRegtestSetup(boolean doMineInitialRegtestBlocks) throws IOException {
        super();
        this.doMineInitialRegtestBlocks = doMineInitialRegtestBlocks;

        rpcConfig = createRpcConfig();
        bitcoindProcess = createBitcoindProcess();

        daemon = createDaemon();
        minerWallet = createNewWallet("miner_wallet");
    }

    @Override
    protected BitcoindRegtestProcess createProcess() {
        return bitcoindProcess;
    }

    @Override
    public void start() throws IOException, InterruptedException {
        super.start();
        initializeZmqListeners();
        initializeWallet(minerWallet);

        if (doMineInitialRegtestBlocks) {
            mineInitialRegtestBlocks();
        }
    }

    @Override
    public void shutdown() {
        bitcoindZeroMq.close();
        loadedWallets.forEach(BitcoindWallet::shutdown);
        super.shutdown();
    }

    public BitcoindWallet createAndInitializeNewWallet(String walletName) throws MalformedURLException {
        var bitcoindWallet = createNewWallet(walletName);
        bitcoindWallet.initialize(Optional.of(WALLET_PASSPHRASE));
        return bitcoindWallet;
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

    @SuppressWarnings("resource")
    private void initializeZmqListeners() {
        var bitcoindRawTxProcessor = new BitcoindRawTxProcessor(daemon, zmqListeners);
        var bitcoindZmqTopicProcessors = new ZmqTopicProcessors(bitcoindRawTxProcessor, zmqListeners);
        bitcoindZeroMq = new ZmqConnection(bitcoindZmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse> zmqNotifications = daemon.getZmqNotifications();
        bitcoindZeroMq.initialize(zmqNotifications);
    }

    private void initializeWallet(BitcoindWallet wallet) {
        wallet.initialize(Optional.of(AbstractRegtestSetup.WALLET_PASSPHRASE));
        loadedWallets.add(wallet);
    }

    public void mineInitialRegtestBlocks() throws InterruptedException {
        mineBlocks(101);
    }

    @Override
    public List<String> mineOneBlock() throws InterruptedException {
        return mineBlocks(1);
    }

    public List<String> mineBlocks(int numberOfBlocks) throws InterruptedException {
        CountDownLatch blocksMinedLatch = registerWaitUntilNBlocksMinedListener(numberOfBlocks);
        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        List<String> blockHashes = daemon.generateToAddress(numberOfBlocks, minerAddress);

        boolean allBlocksMined = blocksMinedLatch.await(15, TimeUnit.SECONDS);
        if (!allBlocksMined) {
            throw new IllegalStateException("Couldn't mine " + numberOfBlocks + " blocks");
        }
        return blockHashes;
    }

    @Override
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

    public Optional<BitcoindListUnspentResponseEntry> filterUtxosByAddress(
            List<BitcoindListUnspentResponseEntry> utxos,
            String address) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getAddress(), address))
                .findFirst();
    }

    public CountDownLatch registerWaitUntilNBlocksMinedListener(int n) {
        var numberOfMinedBlocks = new AtomicInteger();
        CountDownLatch nBlocksMinedLatch = new CountDownLatch(1);

        zmqListeners.registerNewBlockMinedListener((blockHash) -> {
            int minedBlocksCount = numberOfMinedBlocks.incrementAndGet();
            if (minedBlocksCount == n) {
                nBlocksMinedLatch.countDown();
            }
        });
        return nBlocksMinedLatch;
    }

    private RpcConfig createRpcConfig() {
        int port = NetworkUtils.findFreeSystemPort();
        return RpcConfig.builder()
                .hostname("127.0.0.1")
                .user("bisq")
                .password("bisq")
                .port(port)
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
        DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new BitcoindDaemon(rpcClient);
    }
}
