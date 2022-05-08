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
import bisq.wallets.bitcoind.BitcoindRegtestProcess;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.exceptions.RpcCallFailureException;
import bisq.wallets.process.ProcessConfig;
import bisq.wallets.rpc.DaemonRpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class ElementsdRegtestProcess extends BitcoindRegtestProcess {
    private final ElementsdConfig elementsdConfig;

    public ElementsdRegtestProcess(ElementsdConfig elementsdConfig, Path dataDir) {
        super(elementsdConfig.elementsdRpcConfig(), dataDir);
        this.elementsdConfig = elementsdConfig;
    }

    @Override
    public ProcessConfig createProcessConfig() {
        RpcConfig bitcoindRpcConfig = elementsdConfig.bitcoindRpcConfig();
        int zmqPort = NetworkUtils.findFreeSystemPort();
        return new ProcessConfig(
                "elementsd",
                List.of(
                        "-chain=elementsregtest",
                        "-datadir=" + dataDir.toAbsolutePath(),
                        "-debug=1",

                        "-bind=127.0.0.1:" + NetworkUtils.findFreeSystemPort(),
                        "-whitelist=127.0.0.1",

                        "-rpcbind=127.0.0.1:" + rpcConfig.port(),
                        "-rpcallowip=127.0.0.1",
                        "-rpcuser=" + rpcConfig.user(),
                        "-rpcpassword=" + rpcConfig.password(),

                        "-mainchainrpchost=" + bitcoindRpcConfig.hostname(),
                        "-mainchainrpcport=" + bitcoindRpcConfig.port(),
                        "-mainchainrpcuser=" + bitcoindRpcConfig.user(),
                        "-mainchainrpcpassword=" + bitcoindRpcConfig.password(),

                        "-zmqpubhashblock=tcp://127.0.0.1:" + zmqPort,
                        "-zmqpubrawtx=tcp://127.0.0.1:" + zmqPort,

                        "-fallbackfee=0.00000001",
                        "-txindex=1")
        );
    }

    @Override
    public void invokeStopRpcCall() throws IOException {
        try {
            DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
            var chainBackend = new ElementsdDaemon(rpcClient);
            chainBackend.stop();
        } catch (RpcCallFailureException e) {
            log.error("Failed to send stop command to elementsd.", e);
        }
    }
}
