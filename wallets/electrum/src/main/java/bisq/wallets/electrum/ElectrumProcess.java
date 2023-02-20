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
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class ElectrumProcess implements BisqProcess {

    private final Path electrumRootDataDir;
    private final ElectrumProcessConfig processConfig;

    @Getter
    private final Path binaryPath;
    private Optional<ElectrumRegtestProcess> electrumRegtestProcess = Optional.empty();
    private Optional<String> electrumVersion = Optional.empty();


    public ElectrumProcess(Path electrumRootDataDir, ElectrumProcessConfig processConfig) {
        this.electrumRootDataDir = electrumRootDataDir;
        this.processConfig = processConfig;
        binaryPath = resolveBinaryPath();
    }

    @Override
    public void start() {
        if (!binaryPath.toFile().exists()) {
            unpackArchive();
            if (OsUtils.isLinux()) {
                OsUtils.makeBinaryExecutable(binaryPath);
            }
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
        binaryExtractor.extractFileWithSuffix(binarySuffix);
    }

    private void createAndStartProcess() {
        var process = new ElectrumRegtestProcess(binaryPath, processConfig);
        process.start();
        electrumRegtestProcess = Optional.of(process);
    }

    public Optional<String> getElectrumVersion() {
        if (electrumVersion.isPresent()) {
            return electrumVersion;
        }

        String fileName = binaryPath.getFileName().toString();

        // File name: electrum-4.2.2.dmg / electrum-4.2.2.exe / electrum-4.2.2-x86_64.AppImage
        String secondPart = fileName.split("-")[1];
        secondPart = secondPart.replace(".dmg", "")
                .replace(".exe", "");
        electrumVersion = Optional.of(secondPart);

        return Optional.empty();
    }

    public static String getBinarySuffix() {
        switch (OsUtils.getOperatingSystem()) {
            case LINUX:
                return ElectrumBinaryExtractor.LINUX_BINARY_SUFFIX;
            case MAC:
                return ElectrumBinaryExtractor.MAC_OS_BINARY_SUFFIX;
            case WIN:
                return ElectrumBinaryExtractor.WINDOWS_BINARY_SUFFIX;
            default:
                throw new UnsupportedOperationException("Bisq is running on an unsupported OS: " + OsUtils.getOSName());
        }
    }

    public Path getDataDir() {
        return electrumRegtestProcess.orElseThrow().getDataDir();
    }

    public ElectrumDaemon getElectrumDaemon() {
        return electrumRegtestProcess.orElseThrow().getElectrumDaemon();
    }

    private Path resolveBinaryPath() {
        Path destDirPath = electrumRootDataDir.resolve("bin");
        String binarySuffix = getBinarySuffix();

        //todo defined in gradle, we could let gradle write the version to a file which we read
        String version = "4.2.2";
        // File name: electrum-4.2.2.dmg / electrum-4.2.2.exe / electrum-4.2.2-x86_64.AppImage
        switch (OsUtils.getOperatingSystem()) {
            case LINUX:
                return destDirPath.resolve("electrum-" + version + "-x86_64." + binarySuffix);
            case MAC:
                return destDirPath.resolve("Electrum." + binarySuffix)
                        .resolve("Contents/MacOS/run_electrum");
            case WIN:
                return destDirPath.resolve("electrum-" + version + "." + binarySuffix);
            default:
                throw new UnsupportedOperationException("Bisq is running on an unsupported OS: " + OsUtils.getOSName());
        }
    }
}
