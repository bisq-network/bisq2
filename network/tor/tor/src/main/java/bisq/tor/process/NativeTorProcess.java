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

package bisq.tor.process;

import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static bisq.common.facades.FacadeProvider.getJdkFacade;

@Slf4j
public class NativeTorProcess {
    public static final String ARG_OWNER_PID = "__OwningControllerProcess";

    private final Path torDataDirPath;
    private final Path torBinaryPath;
    private final Path torrcPath;
    private Optional<Process> process = Optional.empty();

    public NativeTorProcess(Path torBinaryPath, Path torDataDirPath) {
        this.torBinaryPath = torBinaryPath;
        this.torDataDirPath = torDataDirPath;
        this.torrcPath = torDataDirPath.resolve("torrc");
    }

    public void start() {
        createTorControlDirectory();
        String absoluteTorrcPathAsString = torrcPath.toAbsolutePath().toString();

        String ownerPid = getJdkFacade().getMyPid();
        var processBuilder = new ProcessBuilder(
                torBinaryPath.toAbsolutePath().toString(),
                "--torrc-file", absoluteTorrcPathAsString,
                "--defaults-torrc", absoluteTorrcPathAsString,
                ARG_OWNER_PID, ownerPid
        );

        if (torBinaryPath.startsWith(torDataDirPath)) {
            Map<String, String> environment = processBuilder.environment();
            environment.put("LD_PRELOAD", LdPreload.computeLdPreloadVariable(torDataDirPath));
        }

        getJdkFacade().redirectError(processBuilder);
        getJdkFacade().redirectOutput(processBuilder);

        try {
            Process torProcess = processBuilder.start();
            process = Optional.of(torProcess);
        } catch (IOException e) {
            throw new TorStartupFailedException(e);
        }
    }

    public void waitUntilExited() {
        log.info("Wait until tor process has exited");
        process.ifPresent(process -> {
            try {
                boolean isSuccess = process.waitFor(5, TimeUnit.SECONDS);
                if (!isSuccess) {
                    throw new CouldNotWaitForTorShutdownException("Tor process has not exited after 5 seconds.");
                } else {
                    log.info("Tor process has exited successfully");
                }
            } catch (InterruptedException e) {
                throw new CouldNotWaitForTorShutdownException(e);
            }
        });
    }

    public static Optional<Path> getSystemTorPath() {
        String pathEnvironmentVariable = System.getenv("PATH");
        String[] searchPaths = pathEnvironmentVariable.split(":");

        for (var path : searchPaths) {
            File torBinary = new File(path, "tor");
            if (torBinary.exists()) {
                return Optional.of(torBinary.toPath());
            }
        }

        return Optional.empty();
    }

    private void createTorControlDirectory() {
        File controlDirFile = torDataDirPath.resolve(BaseTorrcGenerator.CONTROL_DIR_NAME).toFile();
        if (!controlDirFile.exists()) {
            boolean isSuccess = controlDirFile.mkdirs();
            if (!isSuccess) {
                throw new TorStartupFailedException("Couldn't create Tor control directory.");
            }
        }
    }
}
