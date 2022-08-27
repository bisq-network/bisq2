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
import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponseEntry;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class BitcoindRegtestSetup
        extends AbstractRegtestSetup<MultiProcessCoordinator, BitcoindWallet> {

    @Getter
    private final RpcConfig rpcConfig;
    private final BitcoindRegtestProcess bitcoindProcess;
    @Getter
    private final RemoteBitcoind remoteBitcoind;

    public BitcoindRegtestSetup() throws IOException {
        this(false);
    }

    public BitcoindRegtestSetup(boolean doMineInitialRegtestBlocks) throws IOException {
        super();
        rpcConfig = createRpcConfig();
        bitcoindProcess = createBitcoindProcess();
        remoteBitcoind = new RemoteBitcoind(tmpDirPath, rpcConfig, doMineInitialRegtestBlocks);
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindProcess, remoteBitcoind)
        );
    }

    public BitcoindWallet createAndInitializeNewWallet(String walletName) throws MalformedURLException {
        return remoteBitcoind.createAndInitializeNewWallet(walletName);
    }

    public void mineInitialRegtestBlocks() throws InterruptedException {
        remoteBitcoind.mineInitialRegtestBlocks();
    }

    @Override
    public List<String> mineOneBlock() throws InterruptedException {
        return mineBlocks(1);
    }

    public List<String> mineBlocks(int numberOfBlocks) throws InterruptedException {
        return remoteBitcoind.mineBlocks(numberOfBlocks);
    }

    @Override
    public void fundWallet(BitcoindWallet receiverWallet, double amount) throws InterruptedException {
        remoteBitcoind.fundWallet(receiverWallet, amount);
    }

    public String fundAddress(String address, double amount) throws InterruptedException {
        return remoteBitcoind.fundAddress(address, amount);
    }

    public String sendBtcAndMineOneBlock(BitcoindWallet senderWallet,
                                         BitcoindWallet receiverWallet,
                                         double amount) throws InterruptedException {
        return remoteBitcoind.sendBtcAndMineOneBlock(senderWallet, receiverWallet, amount);
    }

    public Optional<BitcoindListUnspentResponseEntry> filterUtxosByAddress(
            List<BitcoindListUnspentResponseEntry> utxos,
            String address) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getAddress(), address))
                .findFirst();
    }

    public CountDownLatch waitUntilBlocksMined(List<String> blockHashes) {
        return remoteBitcoind.waitUntilBlocksMined(blockHashes);
    }

    public static RpcConfig createRpcConfig(String hostname, int port) {
        return RpcConfig.builder()
                .hostname(hostname)
                .user("bisq")
                .password("bisq")
                .port(port)
                .build();
    }

    private RpcConfig createRpcConfig() {
        int port = NetworkUtils.findFreeSystemPort();
        return createRpcConfig("127.0.0.1", port);
    }

    private BitcoindRegtestProcess createBitcoindProcess() {
        Path bitcoindDataDir = tmpDirPath.resolve("bitcoind_data_dir");
        return new BitcoindRegtestProcess(
                rpcConfig,
                bitcoindDataDir
        );
    }

    public BitcoindDaemon getDaemon() {
        return remoteBitcoind.getDaemon();
    }

    public BitcoindWallet getMinerWallet() {
        return remoteBitcoind.getMinerWallet();
    }

    public ZmqListeners getZmqListeners() {
        return remoteBitcoind.getZmqListeners();
    }
}
