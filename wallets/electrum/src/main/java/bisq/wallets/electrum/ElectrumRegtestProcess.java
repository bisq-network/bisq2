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

package bisq.wallets.electrum;

import bisq.wallets.core.RpcConfig;
import bisq.wallets.electrum.rpc.ElectrumConfig;
import bisq.wallets.electrum.rpc.cli.ElectrumCli;
import bisq.wallets.electrum.rpc.cli.ElectrumCliFacade;
import bisq.wallets.process.DaemonProcess;
import bisq.wallets.process.ProcessConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ElectrumRegtestProcess extends DaemonProcess {

    private static final String LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION = "connection established. version:";
    private static final String LOG_ELECTRUM_RPC_INTERFACE_READY = "now running and listening. socktype=tcp";

    private final Path binaryPath;
    private final ElectrumConfig electrumConfig;
    private final ElectrumCliFacade electrumCliFacade;

    private boolean waitUntilRpcPortReady;
    @Getter
    private RpcConfig rpcConfig;

    public ElectrumRegtestProcess(Path binaryPath, ElectrumConfig electrumConfig) {
        super(electrumConfig.getDataDir());
        this.binaryPath = binaryPath;
        this.electrumConfig = electrumConfig;

        var electrumCli = new ElectrumCli(binaryPath, dataDir);
        electrumCliFacade = new ElectrumCliFacade(electrumCli);
    }

    @Override
    public void start() {
        super.start();

        electrumCliFacade.enableLoggingToFile();
        electrumCliFacade.setRpcHost(electrumConfig.getRpcHost());
        electrumCliFacade.setRpcPort(electrumConfig.getRpcPort());

        // Restart and wait until JSON-RPC interface ready
        super.shutdown();
        waitUntilRpcPortReady = true;
        super.start();

        rpcConfig = RpcConfig.builder()
                .hostname(electrumConfig.getRpcHost())
                .port(electrumConfig.getRpcPort())
                .user(electrumCliFacade.getRpcUser())
                .password(electrumCliFacade.getRpcPassword())
                .build();
    }

    @Override
    public ProcessConfig createProcessConfig() {
        return ProcessConfig.builder()
                .name(binaryPath.toAbsolutePath().toString())
                .args(List.of(
                        ElectrumCli.ELECTRUM_REGTEST_ARG,
                        "daemon",

                        "-s",
                        electrumConfig.getRpcHost() + ":" + electrumConfig.getElectrumXServerPort() + ":t",

                        ElectrumCli.ELECTRUM_DATA_DIR_ARG,
                        dataDir.toAbsolutePath().toString(),

                        "-v" // Enable logging
                ))
                .environmentVars(Collections.emptyMap())
                .build();
    }

    @Override
    public void invokeStopRpcCall() {
        electrumCliFacade.stop();
    }

    @Override
    protected Set<String> getIsSuccessfulStartUpLogLines() {
        Set<String> linesToMatch = new HashSet<>();
        linesToMatch.add(LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION);

        if (waitUntilRpcPortReady) {
            linesToMatch.add(LOG_ELECTRUM_RPC_INTERFACE_READY);
        }
        return linesToMatch;
    }
}
