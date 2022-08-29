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

import bisq.common.util.FileUtils;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.electrum.rpc.ElectrumProcessConfig;
import bisq.wallets.electrum.rpc.cli.ElectrumCli;
import bisq.wallets.electrum.rpc.cli.ElectrumCliFacade;
import bisq.wallets.process.DaemonProcess;
import bisq.wallets.process.ProcessConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
public class ElectrumRegtestProcess extends DaemonProcess {

    private static final String LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION = "connection established. version:";
    private static final String LOG_ELECTRUM_RPC_INTERFACE_READY = "now running and listening. socktype=tcp";

    private final Path binaryPath;
    private final ElectrumProcessConfig electrumProcessConfig;
    private final ElectrumCliFacade electrumCliFacade;

    @Getter
    private RpcConfig rpcConfig;

    public ElectrumRegtestProcess(Path binaryPath, ElectrumProcessConfig electrumProcessConfig) {
        super(electrumProcessConfig.getDataDir());
        this.binaryPath = binaryPath;
        this.electrumProcessConfig = electrumProcessConfig;

        var electrumCli = new ElectrumCli(binaryPath, dataDir);
        electrumCliFacade = new ElectrumCliFacade(electrumCli);
    }

    @Override
    public void start() {
        createElectrumConfigFile();
        super.start();
        rpcConfig = electrumProcessConfig.getElectrumConfig()
                .toRpcConfig();
    }

    @Override
    public ProcessConfig createProcessConfig() {
        return ProcessConfig.builder()
                .name(binaryPath.toAbsolutePath().toString())
                .args(List.of(
                        ElectrumCli.ELECTRUM_REGTEST_ARG,
                        "daemon",

                        "-s",
                        electrumProcessConfig.getElectrumConfig().getRpcHost() + ":" +
                                electrumProcessConfig.getElectrumXServerPort() + ":t",

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
        return Set.of(
                LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION,
                LOG_ELECTRUM_RPC_INTERFACE_READY
        );
    }

    private void createElectrumConfigFile() {
        try {
            Path regtestDirectory = dataDir.resolve("regtest");
            FileUtils.makeDirs(regtestDirectory.toFile());

            Path configFilePath = regtestDirectory.resolve("config");
            ElectrumConfig electrumConfig = electrumProcessConfig.getElectrumConfig();
            String configAsString = new ObjectMapper().writeValueAsString(electrumConfig);

            Files.writeString(configFilePath, configAsString);
        } catch (IOException e) {
            throw new ElectrumConfigFileCreationFailed(e);
        }
    }
}
