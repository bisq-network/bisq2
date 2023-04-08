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
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.json_rpc.RpcClientFactory;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.ElectrumProcessConfig;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.process.DaemonProcess;
import bisq.wallets.process.ProcessConfig;
import bisq.wallets.process.scanner.FileScanner;
import bisq.wallets.process.scanner.LogScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class ElectrumRegtestProcess extends DaemonProcess {

    private static final String LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION = "connection established. version:";
    private static final String LOG_ELECTRUM_RPC_INTERFACE_READY = "now running and listening. socktype=tcp";

    private final Path binaryPath;
    private final ElectrumProcessConfig electrumProcessConfig;
    @Getter
    private final ElectrumDaemon electrumDaemon;

    @Getter
    private RpcConfig rpcConfig;
    private Future<Path> logFilePath;

    public ElectrumRegtestProcess(Path binaryPath, ElectrumProcessConfig electrumProcessConfig) {
        super(electrumProcessConfig.getDataDir());
        this.binaryPath = binaryPath;
        this.electrumProcessConfig = electrumProcessConfig;
        this.electrumDaemon = createElectrumDaemon();
    }

    @Override
    public void start() {
        // Stop any old running electrum daemon before deleting the old config file.
        stopOld();

        createElectrumConfigFile();
        logFilePath = findNewLogFile();
        super.start();
        rpcConfig = electrumProcessConfig.getElectrumConfig()
                .toRpcConfig();
    }

    public void stopOld() {
        var config = ProcessConfig.builder()
                .name(binaryPath.toAbsolutePath().toString())
                .args(List.of(
                        "--regtest",
                        "--dir",
                        dataDir.toAbsolutePath().toString(),
                        "stop"
                ))
                .environmentVars(Collections.emptyMap())
                .build();
        List<String> args = config.toCommandList();

        var processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);

        Map<String, String> environment = processBuilder.environment();
        environment.putAll(config.getEnvironmentVars());

        try {
            Process process = processBuilder.start();
            String input = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            log.info("Trying to stop old process: " + input);
        } catch (IOException e) {
            log.warn("Exception when stopping old wallet process", e);
        }
    }


    @Override
    public ProcessConfig createProcessConfig() {
        return ProcessConfig.builder()
                .name(binaryPath.toAbsolutePath().toString())
                .args(List.of(
                        "--regtest",
                        "daemon",

                        "-s",
                        electrumProcessConfig.getElectrumXServerHost() + ":" +
                                electrumProcessConfig.getElectrumXServerPort() + ":t",

                        "--dir",
                        dataDir.toAbsolutePath().toString(),

                        "-v" // Enable logging (only works on Mac and Linux)
                ))
                .environmentVars(Collections.emptyMap())
                .build();
    }

    @Override
    public void invokeStopRpcCall() {
        electrumDaemon.stop();
    }

    @Override
    protected LogScanner getLogScanner() {
        return new FileScanner(
                getIsSuccessfulStartUpLogLines(),
                logFilePath
        );
    }

    @Override
    protected Set<String> getIsSuccessfulStartUpLogLines() {
        return Set.of(
                LOG_ELECTRUMX_CONNECTION_ESTABLISHED_VERSION,
                LOG_ELECTRUM_RPC_INTERFACE_READY
        );
    }

    private ElectrumDaemon createElectrumDaemon() {
        RpcConfig rpcConfig = electrumProcessConfig.getElectrumConfig().toRpcConfig();
        JsonRpcClient jsonRpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new ElectrumDaemon(jsonRpcClient);
    }

    private Future<Path> findNewLogFile() {
        try {
            Path logsDirPath = dataDir.resolve("regtest").resolve("logs");
            FileUtils.makeDirs(logsDirPath.toFile());
            var directoryWatcher = new FileCreationWatcher(logsDirPath);
            return directoryWatcher.waitUntilNewFileCreated();
        } catch (IOException e) {
            throw new CannotCreateElectrumLogFileDirectoryException("Cannot create: " + logFilePath, e);
        }
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
            throw new ElectrumConfigFileCreationFailedException(e);
        }
    }
}
