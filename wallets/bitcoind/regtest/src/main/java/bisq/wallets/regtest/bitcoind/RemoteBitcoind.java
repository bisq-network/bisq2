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
import bisq.wallets.core.model.AddressType;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.json_rpc.RpcClientFactory;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.regtest.process.BisqProcess;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static bisq.wallets.regtest.AbstractRegtestSetup.WALLET_PASSPHRASE;

public class RemoteBitcoind implements BisqProcess {

    public static final String MINER_WALLET_NAME = "miner_wallet";

    private final RpcConfig rpcConfig;
    @Getter
    private final BitcoindDaemon daemon;
    @Getter
    private final ZmqListeners zmqListeners = new ZmqListeners();
    @Getter
    private final BitcoindWallet minerWallet;
    private final List<BitcoindWallet> loadedWallets = new ArrayList<>();
    private ZmqConnection bitcoindZeroMq;

    public RemoteBitcoind(RpcConfig rpcConfig) {
        this.rpcConfig = rpcConfig;
        this.daemon = createBitcoindDaemon();
        this.minerWallet = new BitcoindWallet(daemon, rpcConfig, MINER_WALLET_NAME);
    }

    @Override
    public void start() throws InterruptedException {
        initializeZmqListeners();
        initializeWallet(minerWallet);
        mineInitialRegtestBlocks();
    }

    @Override
    public void shutdown() {
        bitcoindZeroMq.close();
        loadedWallets.forEach(BitcoindWallet::shutdown);
    }

    public BitcoindWallet createAndInitializeNewWallet(String walletName) {
        var bitcoindWallet = new BitcoindWallet(daemon, rpcConfig, walletName);
        bitcoindWallet.initialize(Optional.of(WALLET_PASSPHRASE));
        return bitcoindWallet;
    }

    public List<String> mineBlocks(int numberOfBlocks) {
        String minerAddress = minerWallet.getNewAddress(AddressType.BECH32, "");
        return daemon.generateToAddress(numberOfBlocks, minerAddress);
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

    private BitcoindDaemon createBitcoindDaemon() {
        JsonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new BitcoindDaemon(rpcClient);
    }

    private void initializeZmqListeners() {
        var bitcoindRawTxProcessor = new BitcoindRawTxProcessor(daemon, zmqListeners);
        var bitcoindZmqTopicProcessors = new ZmqTopicProcessors(bitcoindRawTxProcessor, zmqListeners);
        bitcoindZeroMq = new ZmqConnection(bitcoindZmqTopicProcessors, zmqListeners);

        List<BitcoindGetZmqNotificationsResponse.Entry> zmqNotifications = daemon.getZmqNotifications();
        bitcoindZeroMq.initialize(zmqNotifications);
    }

    private void initializeWallet(BitcoindWallet wallet) {
        wallet.initialize(Optional.of(WALLET_PASSPHRASE));
        loadedWallets.add(wallet);
    }

    private void mineInitialRegtestBlocks() {
        mineBlocks(101);
    }
}
