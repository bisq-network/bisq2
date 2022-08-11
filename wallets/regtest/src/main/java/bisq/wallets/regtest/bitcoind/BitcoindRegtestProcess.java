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
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.exceptions.RpcCallFailureException;
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.process.DaemonProcess;
import bisq.wallets.process.ProcessConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
public class BitcoindRegtestProcess extends DaemonProcess {

    @Getter
    protected final RpcConfig rpcConfig;

    public BitcoindRegtestProcess(RpcConfig rpcConfig, Path dataDir) {
        super(dataDir);
        this.rpcConfig = rpcConfig;
    }

    @Override
    public ProcessConfig createProcessConfig() {
        int zmqPort = NetworkUtils.findFreeSystemPort();
        return ProcessConfig.builder()
                .name("bitcoind")
                .args(List.of(
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
                        "-txindex=1"))
                .environmentVars(Collections.emptyMap())
                .build();
    }

    @Override
    protected Set<String> getIsSuccessfulStartUpLogLines() {
        return Set.of("init message: Done loading");
    }

    @Override
    public void invokeStopRpcCall() {
        try {
            DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
            var chainBackend = new BitcoindDaemon(rpcClient);
            chainBackend.stop();
        } catch (RpcCallFailureException | MalformedURLException e) {
            log.error("Failed to send stop command to bitcoind.", e);
        }
    }
}
