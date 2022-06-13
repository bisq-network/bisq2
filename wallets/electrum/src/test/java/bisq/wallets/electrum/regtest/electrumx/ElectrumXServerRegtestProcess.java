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

package bisq.wallets.electrum.regtest.electrumx;

import bisq.wallets.core.RpcConfig;
import bisq.wallets.regtest.process.DaemonProcess;
import bisq.wallets.regtest.process.ProcessConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ElectrumXServerRegtestProcess extends DaemonProcess {

    private final ElectrumXServerConfig serverConfig;
    private final ElectrumXRpc electrumXRpc;

    public ElectrumXServerRegtestProcess(ElectrumXServerConfig serverConfig) {
        super(serverConfig.dataDir());
        this.serverConfig = serverConfig;
        electrumXRpc = new ElectrumXRpc(serverConfig.rpcPort());
    }

    @Override
    public ProcessConfig createProcessConfig() {
        return ProcessConfig.builder()
                .name("electrumx_server")
                .args(Collections.emptyList())
                .environmentVars(getEnvironmentVars())
                .build();
    }

    private Map<String, String> getEnvironmentVars() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("SERVICES", "tcp://127.0.0.1:" + serverConfig.port() +
                ",rpc://127.0.0.1:" + serverConfig.rpcPort());
        envMap.put("COIN", "Bitcoin");
        envMap.put("NET", "regtest");

        RpcConfig bitcoindRpcConfig = serverConfig.bitcoindRpcConfig();
        envMap.put("DAEMON_URL",
                "http://" + bitcoindRpcConfig.getUser() + ":" + bitcoindRpcConfig.getPassword() +
                        "@" + bitcoindRpcConfig.getHostname() + ":" + bitcoindRpcConfig.getPort());

        envMap.put("DB_DIRECTORY", dataDir.toAbsolutePath().toString());
        return envMap;
    }

    @Override
    public void invokeStopRpcCall() {
        electrumXRpc.stop();
    }

    @Override
    protected Set<String> getIsSuccessfulStartUpLogLines() {
        return Set.of("TCP server listening on");
    }

    public int getPort() {
        return serverConfig.port();
    }
}
