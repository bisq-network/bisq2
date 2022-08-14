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
import bisq.wallets.core.rpc.DaemonRpcClient;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.electrum.rpc.ElectrumConfig;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.process.BisqProcess;
import bisq.wallets.process.DaemonProcess;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Optional;

public class ElectrumProcess implements BisqProcess {

    private final Path electrumRootDataDir;
    private final ElectrumConfig config;

    @Getter
    private Optional<Path> binaryPath = Optional.empty();
    private Optional<ElectrumRegtestProcess> electrumRegtestProcess = Optional.empty();
    @Getter
    private Optional<ElectrumDaemon> electrumDaemon = Optional.empty();
    private Optional<String> electrumVersion = Optional.empty();


    public ElectrumProcess(Path electrumRootDataDir, ElectrumConfig config) {
        this.electrumRootDataDir = electrumRootDataDir;
        this.config = config;
    }

    @Override
    public void start() {
        unpackArchive();
        if (isRunningOnLinux()) {
            makeBinaryExecutable();
        }
        createAndStartProcess();

        electrumDaemon = Optional.of(createElectrumDaemon());
    }

    @Override
    public void shutdown() {
        electrumRegtestProcess.ifPresent(DaemonProcess::shutdown);
    }

    private void unpackArchive() {
        Path destDirPath = electrumRootDataDir.resolve("bin");
        var binaryExtractor = new ElectrumBinaryExtractor(destDirPath);

        String binarySuffix = getBinarySuffix();
        Path extractedFilePath = binaryExtractor.extractFileWithSuffix(binarySuffix);
        binaryPath = Optional.of(extractedFilePath);
    }

    private boolean isRunningOnLinux() {
        String osName = System.getProperty("os.name");
        return osName.equals("Linux");
    }

    private void makeBinaryExecutable() {
        Path binaryPath = this.binaryPath.orElseThrow();
        boolean isSuccess = binaryPath.toFile().setExecutable(true);
        if (!isSuccess) {
            throw new IllegalStateException(
                    String.format("Couldn't make `%s` executable.", binaryPath)
            );
        }
    }

    private void createAndStartProcess() {
        Path path = binaryPath.orElseThrow();
        var process = new ElectrumRegtestProcess(path, config);
        process.start();
        electrumRegtestProcess = Optional.of(process);
    }

    private ElectrumDaemon createElectrumDaemon() {
        RpcConfig rpcConfig = electrumRegtestProcess.orElseThrow().getRpcConfig();
        DaemonRpcClient daemonRpcClient = RpcClientFactory.createDaemonRpcClient(rpcConfig);
        return new ElectrumDaemon(daemonRpcClient);
    }

    public Optional<String> getElectrumVersion() {
        if (electrumVersion.isPresent()) {
            return electrumVersion;
        }

        if (binaryPath.isPresent()) {
            Path path = binaryPath.get();
            String fileName = path.getFileName().toString();

            // File name: electrum-4.2.2.dmg / electrum-4.2.2-portable.exe / electrum-4.2.2-x86_64.AppImage
            String secondPart = fileName.split("-")[1];
            secondPart = secondPart.replace(".dmg", "");
            electrumVersion = Optional.of(secondPart);
        }

        return Optional.empty();
    }

    private String getBinarySuffix() {
        String osName = System.getProperty("os.name");
        if (isRunningOnLinux()) {
            return ElectrumBinaryExtractor.LINUX_BINARY_SUFFIX;
        }
        throw new UnsupportedOperationException("Unsupported OS: " + osName);
    }

    public Path getDataDir() {
        return electrumRegtestProcess.orElseThrow().getDataDir();
    }
}
