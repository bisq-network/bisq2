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

package bisq.wallets.electrum.regtest.electrum;

import bisq.wallets.core.RpcConfig;
import bisq.wallets.electrum.regtest.electrum.cli.ElectrumCli;
import bisq.wallets.electrum.regtest.electrum.cli.ElectrumCliFacade;
import bisq.wallets.regtest.process.DaemonProcess;
import bisq.wallets.regtest.process.ProcessConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ElectrumRegtestProcess extends DaemonProcess {

    private static final String LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION = "connection established. version:";
    private static final String LOG_ELECTRUM_RPC_INTERFACE_READY = "now running and listening. socktype=tcp";

    private final int electrumXServerPort;
    private final RpcHostSpec rpcHostSpec;
    private final ElectrumCliFacade electrumCliFacade;

    private boolean waitUntilRpcPortReady;
    @Getter
    private RpcConfig rpcConfig;

    public ElectrumRegtestProcess(int electrumXServerPort, RpcHostSpec rpcHostSpec, Path dataDir) {
        super(dataDir);
        this.electrumXServerPort = electrumXServerPort;
        this.rpcHostSpec = rpcHostSpec;

        var electrumCli = new ElectrumCli(dataDir);
        electrumCliFacade = new ElectrumCliFacade(electrumCli);
    }

    @Override
    public void start() throws IOException, InterruptedException {
        super.start();

        electrumCliFacade.setRpcHost(rpcHostSpec.host());
        electrumCliFacade.setRpcPort(rpcHostSpec.port());

        // Restart and wait until JSON-RPC interface ready
        super.shutdown();
        waitUntilRpcPortReady = true;
        super.start();

        rpcConfig = RpcConfig.builder()
                .hostname(rpcHostSpec.host())
                .port(rpcHostSpec.port())
                .user(electrumCliFacade.getRpcUser())
                .password(electrumCliFacade.getRpcPassword())
                .build();
    }

    @Override
    public ProcessConfig createProcessConfig() {
        return ProcessConfig.builder()
                .name(ElectrumCli.ELECTRUM_BINARY_NAME)
                .args(List.of(
                        ElectrumCli.ELECTRUM_REGTEST_ARG,
                        "daemon",

                        "-s",
                        "localhost:" + electrumXServerPort + ":t",

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
