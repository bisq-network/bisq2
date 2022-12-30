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

import bisq.common.util.OsUtils;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.ElectrumProcessConfig;
import bisq.wallets.process.BisqProcess;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Optional;

public class ElectrumProcess implements BisqProcess {

    private final Path electrumRootDataDir;
    private final ElectrumProcessConfig processConfig;

    @Getter
    private Optional<Path> binaryPath = Optional.empty();
    private Optional<ElectrumRegtestProcess> electrumRegtestProcess = Optional.empty();
    private Optional<String> electrumVersion = Optional.empty();


    public ElectrumProcess(Path electrumRootDataDir, ElectrumProcessConfig processConfig) {
        this.electrumRootDataDir = electrumRootDataDir;
        this.processConfig = processConfig;
    }

    @Override
    public void start() {
        unpackArchive();
        if (OsUtils.isLinux()) {
            OsUtils.makeBinaryExecutable(binaryPath.orElseThrow());
        }
        createAndStartProcess();
    }

    @Override
    public void shutdown() {
        electrumRegtestProcess.ifPresent(ElectrumRegtestProcess::invokeStopRpcCall);
    }

    private void unpackArchive() {
        Path destDirPath = electrumRootDataDir.resolve("bin");
        var binaryExtractor = new ElectrumBinaryExtractor(destDirPath);

        String binarySuffix = getBinarySuffix();
        Path extractedFilePath = binaryExtractor.extractFileWithSuffix(binarySuffix);

        if (OsUtils.isOSX()) {
            extractedFilePath = extractedFilePath.resolve("Contents/MacOS/run_electrum");
        }

        binaryPath = Optional.of(extractedFilePath);
    }

    private void createAndStartProcess() {
        Path path = binaryPath.orElseThrow();
        var process = new ElectrumRegtestProcess(path, processConfig);
        process.start();
        electrumRegtestProcess = Optional.of(process);
    }

    public Optional<String> getElectrumVersion() {
        if (electrumVersion.isPresent()) {
            return electrumVersion;
        }

        if (binaryPath.isPresent()) {
            Path path = binaryPath.get();
            String fileName = path.getFileName().toString();

            // File name: electrum-4.2.2.dmg / electrum-4.2.2.exe / electrum-4.2.2-x86_64.AppImage
            String secondPart = fileName.split("-")[1];
            secondPart = secondPart.replace(".dmg", "");
            electrumVersion = Optional.of(secondPart);
        }

        return Optional.empty();
    }

    public static String getBinarySuffix() {
        if (OsUtils.isLinux()) {
            return ElectrumBinaryExtractor.LINUX_BINARY_SUFFIX;
        } else if (OsUtils.isOSX()) {
            return ElectrumBinaryExtractor.MAC_OS_BINARY_SUFFIX;
        } else if (OsUtils.isWindows()) {
            return ElectrumBinaryExtractor.WINDOWS_BINARY_SUFFIX;
        }

        throw new UnsupportedOperationException("Bisq is running on an unsupported OS: " + OsUtils.getOSName());
    }

    public Path getDataDir() {
        return electrumRegtestProcess.orElseThrow().getDataDir();
    }

    public ElectrumDaemon getElectrumDaemon() {
        return electrumRegtestProcess.orElseThrow().getElectrumDaemon();
    }
}
