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
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.json_rpc.RpcCallFailureException;
import bisq.wallets.json_rpc.RpcClientFactory;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.regtest.process.DaemonProcess;
import bisq.wallets.regtest.process.ProcessConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Slf4j
public class BitcoindRegtestProcess extends DaemonProcess {

    private final Path binaryPath;
    protected final int p2pPort;
    @Getter
    protected final RpcConfig rpcConfig;
    private final BitcoindDaemon bitcoindDaemon;

    public BitcoindRegtestProcess(Path binaryPath, int p2pPort, RpcConfig rpcConfig, Path dataDir) {
        super(dataDir);
        this.binaryPath = binaryPath;
        this.p2pPort = p2pPort;
        this.rpcConfig = rpcConfig;
        JsonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        bitcoindDaemon = new BitcoindDaemon(rpcClient);
    }

    @Override
    public ProcessConfig createProcessConfig() {
        int zmqPort = BitcoindRegtestSetup.findFreeSystemPort();
        return ProcessConfig.builder()
                .name(binaryPath.toAbsolutePath().toString())
                .args(List.of(
                        "-regtest",
                        "-datadir=" + dataDir.toAbsolutePath(),
                        "-debug=1",

                        "-bind=127.0.0.1:" + p2pPort,
                        "-whitelist=127.0.0.1",

                        "-rpcbind=127.0.0.1:" + rpcConfig.getPort(),
                        "-rpcallowip=127.0.0.1",
                        "-rpcuser=" + rpcConfig.getUser(),
                        "-rpcpassword=" + rpcConfig.getPassword(),

                        "-zmqpubhashblock=tcp://127.0.0.1:" + zmqPort,
                        "-zmqpubrawtx=tcp://127.0.0.1:" + zmqPort,

                        "-fallbackfee=0.00000001",
                        "-txindex=1",
                        "-peerbloomfilters=1"))
                .environmentVars(Collections.emptyMap())
                .build();
    }

    @Override
    protected void waitUntilReady() {
        Instant timeoutInstant = Instant.now().plus(2, ChronoUnit.MINUTES);
        int failedAttempts = 0;
        while (true) {
            try {
                bitcoindDaemon.listWallets();
                log.info("Connected to Bitcoin Core.");
                break;
            } catch (RpcCallFailureException e) {
                if (e.getCause() instanceof ConnectException) {
                    if (isAfterTimeout(timeoutInstant)) {
                        throw new IllegalStateException("Bitcoin Core isn't ready after 2 minutes. Giving up.");
                    }

                    failedAttempts++;
                    double msToWait = Math.min(300 * failedAttempts, 10_000);
                    log.info("Bitcoind RPC isn't ready yet. Trying again in {}ms.", msToWait);
                    sleepForMs(msToWait);
                }
            }
        }
    }

    private boolean isAfterTimeout(Instant timeoutInstant) {
        return Instant.now().isAfter(timeoutInstant);
    }

    private void sleepForMs(double ms) {
        try {
            Thread.sleep((long) ms);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void invokeStopRpcCall() {
        try {
            bitcoindDaemon.stop();
        } catch (RpcCallFailureException e) {
            log.error("Failed to send stop command to bitcoind.", e);
        }
    }
}
