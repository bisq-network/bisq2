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

import bisq.common.FileCreationWatcher;
import bisq.common.scanner.FileScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
public class NativeTorProcess {
    public static final String ARG_OWNER_PID = "__OwningControllerProcess";

    private final Path torDataDirPath;
    private final Path torBinaryPath;
    private final Path torrcPath;
    private Optional<Process> process = Optional.empty();
    private Optional<Future<Path>> logFileCreationWaiter = Optional.empty();

    public NativeTorProcess(Path torDataDirPath) {
        this.torDataDirPath = torDataDirPath;
        this.torBinaryPath = torDataDirPath.resolve("tor");
        this.torrcPath = torDataDirPath.resolve("torrc");
    }

    public void start() {
        String absoluteTorrcPathAsString = torrcPath.toAbsolutePath().toString();

        String ownerPid = Pid.getMyPid();
        var processBuilder = new ProcessBuilder(
                torBinaryPath.toAbsolutePath().toString(),
                "-f", absoluteTorrcPathAsString,
                ARG_OWNER_PID, ownerPid
        );

        Map<String, String> environment = processBuilder.environment();
        environment.put("LD_PRELOAD", computeLdPreloadVariable());

        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        logFileCreationWaiter = Optional.of(createLogFileCreationWaiter());

        try {
            Process torProcess = processBuilder.start();
            process = Optional.of(torProcess);
        } catch (IOException e) {
            throw new TorStartupFailedException(e);
        }
    }

    public void waitUntilControlPortReady() {
        try {
            if (logFileCreationWaiter.isPresent()) {
                Future<Path> pathFuture = logFileCreationWaiter.get();

                FileScanner fileScanner = new FileScanner(
                        Set.of("[notice] Opened Control listener connection (ready) on "),
                        pathFuture
                );
                fileScanner.waitUntilLogContainsLines();
            }

        } catch (ExecutionException | IOException | InterruptedException | TimeoutException e) {
            log.error("Couldn't wait for log file creation.", e);
            throw new IllegalStateException("Couldn't wait for log file creation.");
        }
    }

    public void waitUntilExited() {
        process.ifPresent(process -> {
            try {
                boolean isSuccess = process.waitFor(2, TimeUnit.MINUTES);
                if (!isSuccess) {
                    throw new CouldNotWaitForTorShutdownException("Tor still running after 2 minutes timeout.");
                }
            } catch (InterruptedException e) {
                throw new CouldNotWaitForTorShutdownException(e);
            }
        });
    }

    private String computeLdPreloadVariable() {
        File[] sharedLibraries = torDataDirPath.toFile()
                .listFiles((file, fileName) -> fileName.contains(".so."));
        Objects.requireNonNull(sharedLibraries);

        return Arrays.stream(sharedLibraries)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(":"));
    }

    private Future<Path> createLogFileCreationWaiter() {
        Path dataDir = torrcPath.getParent();
        Path logFilePath = torrcPath.getParent().resolve("debug.log");

        FileCreationWatcher fileCreationWatcher = new FileCreationWatcher(dataDir);
        return fileCreationWatcher.waitForFile(logFilePath);
    }

}
