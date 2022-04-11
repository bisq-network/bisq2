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

package bisq.wallets.elementsd.process;

import bisq.common.util.NetworkUtils;
import bisq.wallets.NetworkType;
import bisq.wallets.bitcoind.BitcoindProcess;
import bisq.wallets.elementsd.ElementsdConfig;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.exceptions.RpcCallFailureException;
import bisq.wallets.process.ProcessConfig;
import bisq.wallets.rpc.RpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class ElementsdProcess extends BitcoindProcess {
    private final ElementsdConfig elementsdConfig;

    public ElementsdProcess(ElementsdConfig elementsdConfig, Path dataDir) {
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
                        "-chain=" + getNetworkName(),
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
            RpcClient rpcClient = RpcClientFactory.create(rpcConfig);
            var chainBackend = new ElementsdDaemon(rpcClient);
            chainBackend.stop();
        } catch (RpcCallFailureException e) {
            log.error("Failed to send stop command to elementsd.", e);
        }
    }

    private String getNetworkName() {
        NetworkType networkType = rpcConfig.networkType();
        return switch (networkType) {
            case MAINNET -> "";
            case REGTEST -> "elementsregtest";
            case SIGNET -> "signet";
            case TESTNET -> "liquidv1test";
        };
    }
}
