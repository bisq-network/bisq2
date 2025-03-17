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

import bisq.common.util.NetworkUtils;
import bisq.wallets.json_rpc.RpcCallFailureException;
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.json_rpc.RpcClientFactory;
import bisq.wallets.elementsd.ElementsdConfig;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.regtest.process.ProcessConfig;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestProcess;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ElementsdRegtestProcess extends BitcoindRegtestProcess {
    private final ElementsdConfig elementsdConfig;

    public ElementsdRegtestProcess(ElementsdConfig elementsdConfig, Path dataDir) {
        super(null, NetworkUtils.findFreeSystemPort(), elementsdConfig.elementsdRpcConfig(), dataDir);
        this.elementsdConfig = elementsdConfig;
    }

    @Override
    public ProcessConfig createProcessConfig() {
        RpcConfig bitcoindRpcConfig = elementsdConfig.bitcoindRpcConfig();
        int zmqPort = NetworkUtils.findFreeSystemPort();
        return ProcessConfig.builder()
                .name("elementsd")
                .args(List.of(
                        "-chain=elementsregtest",
                        "-datadir=" + dataDir.toAbsolutePath(),
                        "-debug=1",

                        "-bind=127.0.0.1:" + p2pPort,
                        "-whitelist=127.0.0.1",

                        "-rpcbind=127.0.0.1:" + rpcConfig.getPort(),
                        "-rpcallowip=127.0.0.1",
                        "-rpcuser=" + rpcConfig.getUser(),
                        "-rpcpassword=" + rpcConfig.getPassword(),

                        "-mainchainrpchost=" + bitcoindRpcConfig.getHostname(),
                        "-mainchainrpcport=" + bitcoindRpcConfig.getPort(),
                        "-mainchainrpcuser=" + bitcoindRpcConfig.getUser(),
                        "-mainchainrpcpassword=" + bitcoindRpcConfig.getPassword(),

                        "-zmqpubhashblock=tcp://127.0.0.1:" + zmqPort,
                        "-zmqpubrawtx=tcp://127.0.0.1:" + zmqPort,

                        "-fallbackfee=0.00000001",
                        "-txindex=1"))
                .environmentVars(Collections.emptyMap())
                .build();
    }

    @Override
    public void invokeStopRpcCall() {
        try {
            JsonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
            var chainBackend = new ElementsdDaemon(rpcClient);
            chainBackend.stop();
        } catch (RpcCallFailureException e) {
            log.error("Failed to send stop command to elementsd.", e);
        }
    }
}
