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
import bisq.wallets.bitcoind.rpc.responses.BitcoindListUnspentResponse;
import bisq.wallets.bitcoind.zmq.ZmqListeners;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.Os;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public class BitcoindRegtestSetup
        extends AbstractRegtestSetup<MultiProcessCoordinator> {

    @Getter
    private final RpcConfig rpcConfig;
    private final BitcoindRegtestProcess bitcoindProcess;
    @Getter
    private final RemoteBitcoind remoteBitcoind;

    public BitcoindRegtestSetup() throws IOException {
        super();
        rpcConfig = createRpcConfig();
        bitcoindProcess = createBitcoindProcess();
        remoteBitcoind = new RemoteBitcoind(rpcConfig);
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(bitcoindProcess, remoteBitcoind)
        );
    }

    public BitcoindWallet createAndInitializeNewWallet(String walletName) {
        return remoteBitcoind.createAndInitializeNewWallet(walletName);
    }

    @Override
    public List<String> mineOneBlock() throws InterruptedException {
        return mineBlocks(1);
    }

    public List<String> mineBlocks(int numberOfBlocks) {
        return remoteBitcoind.mineBlocks(numberOfBlocks);
    }

    public String fundAddress(String address, double amount) throws InterruptedException {
        return remoteBitcoind.fundAddress(address, amount);
    }

    public String sendBtcAndMineOneBlock(BitcoindWallet senderWallet,
                                         BitcoindWallet receiverWallet,
                                         double amount) throws InterruptedException {
        return remoteBitcoind.sendBtcAndMineOneBlock(senderWallet, receiverWallet, amount);
    }

    public Optional<BitcoindListUnspentResponse.Entry> filterUtxosByAddress(
            List<BitcoindListUnspentResponse.Entry> utxos,
            String address) {
        return utxos.stream()
                .filter(u -> Objects.equals(u.getAddress(), address))
                .findFirst();
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
        int port = findFreeSystemPort();
        return createRpcConfig("127.0.0.1", port);
    }

    private BitcoindRegtestProcess createBitcoindProcess() throws IOException {
        Path bitcoindBinaryDir = tmpDirPath.resolve("bitcoind_binary");
        Path bitcoindPath = installBitcoind(bitcoindBinaryDir);

        Path bitcoindDataDir = tmpDirPath.resolve("bitcoind_data_dir");
        return new BitcoindRegtestProcess(
                bitcoindPath,
                rpcConfig,
                bitcoindDataDir
        );
    }

    public Path getDataDir() {
        return bitcoindProcess.getDataDir();
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

    private Path installBitcoind(Path bitcoindBinaryDir) throws IOException {
        File bitcoindBinaryDirFile = bitcoindBinaryDir.toFile();
        boolean isSuccess = bitcoindBinaryDirFile.mkdirs();
        if (!isSuccess) {
            throw new IllegalStateException("Couldn't create " + bitcoindBinaryDir.toAbsolutePath() + " for " +
                    "bitcoind installation.");
        }

        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("bitcoind");

        if (inputStream == null) {
            throw new IllegalStateException("Can't read bitcoind binary from resources.");
        }

        Path bitcoindPath = bitcoindBinaryDir.resolve("bitcoind");
        try (inputStream) {
            Files.copy(inputStream, bitcoindPath);

            boolean endOfStreamReached = inputStream.available() == 0;
            if (!endOfStreamReached) {
                throw new IllegalStateException("Couldn't extract bitcoind binary.");
            }

            if (Os.isLinux() || Os.isMacOs()) {
                isSuccess = bitcoindPath.toFile().setExecutable(true);
                if (!isSuccess) {
                    throw new IllegalStateException("Couldn't set executable bit on bitcoind binary.");
                }
            }

            return bitcoindPath;
        }
    }

    public static int findFreeSystemPort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException ignored) {
            return new Random().nextInt(10000) + 50000;
        }
    }
}
