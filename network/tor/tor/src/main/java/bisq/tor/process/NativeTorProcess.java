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
import bisq.tor.Pid;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class NativeTorProcess {

    public static final String ARG_OWNER_PID = "__OwningControllerProcess";
    private final Path torrcPath;
    private Optional<Process> process = Optional.empty();
    private Optional<Future<Path>> logFileCreationWaiter = Optional.empty();

    public NativeTorProcess(Path torrcPath) {
        this.torrcPath = torrcPath;
    }

    public void start() {
        String absoluteTorrcPathAsString = torrcPath.toAbsolutePath().toString();

        String ownerPid = Pid.getMyPid();
        var processBuilder = new ProcessBuilder("tor",
                "-f", absoluteTorrcPathAsString,
                ARG_OWNER_PID, ownerPid
        );

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

    private Future<Path> createLogFileCreationWaiter() {
        Path dataDir = torrcPath.getParent();
        Path logFilePath = torrcPath.getParent().resolve("debug.log");

        FileCreationWatcher fileCreationWatcher = new FileCreationWatcher(dataDir);
        return fileCreationWatcher.waitForFile(logFilePath);
    }

}
