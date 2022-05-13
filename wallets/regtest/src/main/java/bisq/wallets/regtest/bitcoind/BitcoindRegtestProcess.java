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

import bisq.common.util.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.exceptions.RpcCallFailureException;
import bisq.wallets.core.exceptions.WalletShutdownFailedException;
import bisq.wallets.core.exceptions.WalletStartupFailedException;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.regtest.process.DaemonProcess;
import bisq.wallets.regtest.process.ProcessConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

@Slf4j
public class BitcoindRegtestProcess implements DaemonProcess {

    @Getter
    protected final RpcConfig rpcConfig;
    protected final Path dataDir;

    private Process bitcoindProcess;

    public BitcoindRegtestProcess(RpcConfig rpcConfig, Path dataDir) {
        this.rpcConfig = rpcConfig;
        this.dataDir = dataDir;
    }

    @Override
    public ProcessConfig createProcessConfig() {
        int zmqPort = NetworkUtils.findFreeSystemPort();
        return new ProcessConfig(
                "bitcoind",
                List.of(
                        "-regtest",
                        "-datadir=" + dataDir.toAbsolutePath(),
                        "-debug=1",

                        "-bind=127.0.0.1:" + NetworkUtils.findFreeSystemPort(),
                        "-whitelist=127.0.0.1",

                        "-rpcbind=127.0.0.1:" + rpcConfig.getPort(),
                        "-rpcallowip=127.0.0.1",
                        "-rpcuser=" + rpcConfig.getUser(),
                        "-rpcpassword=" + rpcConfig.getPassword(),

                        "-zmqpubhashblock=tcp://127.0.0.1:" + zmqPort,
                        "-zmqpubrawtx=tcp://127.0.0.1:" + zmqPort,

                        "-fallbackfee=0.00000001",
                        "-txindex=1")
        );
    }

    @Override
    public void start() {
        try {
            FileUtils.makeDirs(dataDir.toFile());
            bitcoindProcess = createAndStartProcess();
            waitUntilReady();
        } catch (IOException e) {
            throw new WalletStartupFailedException("Cannot start wallet process.", e);
        }
    }

    @Override
    public void invokeStopRpcCall() throws IOException {
        try {
            DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
            var chainBackend = new BitcoindDaemon(rpcClient);
            chainBackend.stop();
        } catch (RpcCallFailureException e) {
            log.error("Failed to send stop command to bitcoind.", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            invokeStopRpcCall();
            bitcoindProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new WalletShutdownFailedException("Cannot shutdown wallet process.", e);
        }
    }

    private Process createAndStartProcess() throws IOException {
        ProcessConfig processConfig = createProcessConfig();
        return new ProcessBuilder(processConfig.toCommandList())
                .start();
    }

    private void waitUntilReady() {
        waitUntilLogFileContainsLine("init message: Done loading");
    }

    private void waitUntilLogFileContainsLine(String lineToMatch) {
        try (Scanner scanner = new Scanner(bitcoindProcess.getInputStream())) {
            while (scanner.hasNextLine()) {
                // The bitcoind log starts with a timestamp, so we check only for expected string with `endsWith`
                if (scanner.nextLine().endsWith(lineToMatch)) {
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Bitcoind didn't start correctly.", e);
            throw e;
        }
    }
}
