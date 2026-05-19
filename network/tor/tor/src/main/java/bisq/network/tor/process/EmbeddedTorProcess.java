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

package bisq.network.tor.process;

import bisq.common.file.FileMutatorUtils;
import bisq.network.tor.common.torrc.BaseTorrcGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static bisq.common.facades.FacadeProvider.getJdkFacade;

@Slf4j
public class EmbeddedTorProcess {
    public static final String ARG_OWNER_PID = "__OwningControllerProcess";

    private final Path torDataDirPath;
    private final Path torBinaryPath;
    private final Path torrcPath;
    private final Path stdoutLogPath;
    private final Path stderrLogPath;
    private Optional<Process> process = Optional.empty();

    public EmbeddedTorProcess(Path torBinaryPath, Path torDataDirPath) {
        this.torBinaryPath = torBinaryPath;
        this.torDataDirPath = torDataDirPath;
        this.torrcPath = torDataDirPath.resolve("torrc");
        this.stdoutLogPath = torDataDirPath.resolve("tor-stdout.log");
        this.stderrLogPath = torDataDirPath.resolve("tor-stderr.log");
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
            String ldPreload = LdPreload.computeLdPreloadVariable(torDataDirPath);
            Map<String, String> environment = processBuilder.environment();
            if (!ldPreload.isBlank()) {
                environment.put("LD_PRELOAD", ldPreload);
            }
        }

        processBuilder.redirectError(stderrLogPath.toFile());
        processBuilder.redirectOutput(stdoutLogPath.toFile());

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
                log.warn("Thread got interrupted at waitUntilExited method", e);
                Thread.currentThread().interrupt(); // Restore interrupted state
                throw new CouldNotWaitForTorShutdownException(e);
            }
        });
    }

    public void shutdown() {
        process.ifPresent(process -> {
            if (!process.isAlive()) {
                return;
            }

            process.destroy();
            try {
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (!exited && process.isAlive()) {
                    destroyForciblyAndWait(process);
                }
            } catch (InterruptedException e) {
                log.warn("Thread got interrupted at shutdown method", e);
                if (process.isAlive()) {
                    destroyForciblyAndWait(process);
                }
                Thread.currentThread().interrupt();
            }
        });
    }

    private void destroyForciblyAndWait(Process process) {
        process.destroyForcibly();
        try {
            boolean exited = process.waitFor(3, TimeUnit.SECONDS);
            if (!exited && process.isAlive()) {
                log.warn("Tor process {} did not exit after being forcibly destroyed", process.pid());
            }
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted while waiting for forcibly destroyed Tor process to exit", e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isAlive() {
        return process.map(Process::isAlive).orElse(false);
    }

    public Optional<Integer> getExitCode() {
        return process.flatMap(process -> {
            if (process.isAlive()) {
                return Optional.empty();
            }
            return Optional.of(process.exitValue());
        });
    }

    public String getStartupDiagnostics() {
        String exitCode = getExitCode()
                .map(String::valueOf)
                .orElseGet(() -> isAlive() ? "still running" : "not started");
        return "exitCode=" + exitCode +
                ", stderrLog=" + stderrLogPath.toAbsolutePath() +
                ", stderr=" + readLogSnippet(stderrLogPath);
    }

    public static Optional<Path> getSystemTorPath() {
        String pathEnvironmentVariable = System.getenv("PATH");
        String[] searchPaths = pathEnvironmentVariable.split(":");

        for (var path : searchPaths) {
            Path torBinaryPath = Paths.get(path, "tor");
            if (Files.exists(torBinaryPath)) {
                return Optional.of(torBinaryPath);
            }
        }

        return Optional.empty();
    }

    private void createTorControlDirectory() {
        Path controlDirFilePath = torDataDirPath.resolve(BaseTorrcGenerator.CONTROL_DIR_NAME);
        if (!Files.exists(controlDirFilePath)) {
            try {
                FileMutatorUtils.createDirectories(controlDirFilePath);
            } catch (IOException e) {
                throw new TorStartupFailedException("Couldn't create Tor control directory.");
            }
        }
    }

    private String readLogSnippet(Path logPath) {
        try {
            if (!Files.exists(logPath)) {
                return "<missing>";
            }
            String content = Files.readString(logPath, StandardCharsets.UTF_8).trim();
            int maxLength = 4000;
            if (content.length() > maxLength) {
                return content.substring(content.length() - maxLength);
            }
            return content.isEmpty() ? "<empty>" : content;
        } catch (IOException e) {
            return "<failed to read " + logPath.toAbsolutePath() + ": " + e.getMessage() + ">";
        }
    }
}
