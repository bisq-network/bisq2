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

package bisq.desktop.webcam;

import bisq.common.file.FileMutatorUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public class WebcamProcessLauncher {
    private final Path webcamDirPath;
    private final WebcamJarProvider webcamJarProvider;
    private Optional<LauncherState> launcherState = Optional.empty();

    public WebcamProcessLauncher(Path appDataDirPath) {
        this.webcamDirPath = appDataDirPath.resolve("webcam");
        this.webcamJarProvider = new WebcamJarProvider(webcamDirPath);
    }

    public CompletableFuture<Process> start(String sessionSecret) {
        LauncherState startingState = setStartingState();
        ExecutorService launchExecutor;
        try {
            launchExecutor = ExecutorFactory.newSingleThreadExecutor("WebcamProcessLauncher");
        } catch (RuntimeException e) {
            getAndClearLauncherStateIfCurrent(startingState);
            throw e;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                ensureCurrentLauncherState(startingState);
                Path jarFilePath = webcamJarProvider.prepareWebcamJar();
                String languageTagParam = "--languageTag=" + LanguageRepository.getDefaultLanguageTag();

                String pathToJavaExe = System.getProperty("java.home") + "/bin/java";
                ProcessBuilder processBuilder;
                if (OS.isMacOs()) {
                    String iconPath = webcamDirPath + "/webcam-app-icon.png";
                    Path bisqIconPath = Paths.get(iconPath);
                    if (!Files.exists(bisqIconPath)) {
                        FileMutatorUtils.resourceToFile("images/webcam/webcam-app-icon@2x.png", bisqIconPath);
                    }
                    String jvmArgs = "-Xdock:icon=" + iconPath;
                    processBuilder = new ProcessBuilder(pathToJavaExe, jvmArgs, "-jar", jarFilePath.toAbsolutePath().toString(), languageTagParam);
                } else {
                    processBuilder = new ProcessBuilder(pathToJavaExe, "-jar", jarFilePath.toAbsolutePath().toString(), languageTagParam);
                }
                // Stdout is reserved for framed webcam IPC. Stderr is reserved for child process logs.
                processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);

                ensureCurrentLauncherState(startingState);
                log.info("Launching webcam app process");
                Process process = processBuilder.start();
                Optional<WebcamProcessLogReader> logReader = Optional.empty();
                Optional<LauncherState> runningState = Optional.empty();
                try {
                    logReader = Optional.of(WebcamProcessLogReader.start(process.getErrorStream(), webcamDirPath.resolve("webcam-app")));
                    runningState = setRunningState(startingState, process, logReader.get());
                    if (runningState.isEmpty()) {
                        throw new CancellationException("Webcam app process launch was cancelled");
                    }
                    sendSessionSecret(process, sessionSecret);
                    ensureCurrentLauncherState(runningState.get());
                } catch (Exception e) {
                    clearFailedStartupState(process, logReader, runningState);
                    throw e;
                }
                log.info("Webcam app process successfully launched");
                return process;
            } catch (CancellationException e) {
                log.info("Webcam app process launch cancelled");
                throw e;
            } catch (Exception e) {
                getAndClearLauncherStateIfCurrent(startingState);
                log.error("Launching process failed", e);
                throw new RuntimeException(e);
            }
        }, launchExecutor).whenComplete((process, throwable) -> launchExecutor.shutdown());
    }

    private void destroyFailedStartupProcess(Process process) {
        process.destroyForcibly();
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Thread got interrupted while waiting after failed startup shutdown", e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendSessionSecret(Process process, String sessionSecret) {
        // Child stdin is reserved for this one-shot IPC secret bootstrap.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(sessionSecret);
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("Sending webcam IPC session secret failed", e);
        }
    }

    public CompletableFuture<Boolean> shutdown() {
        Optional<LauncherState> launcherState = getAndClearLauncherState();
        return CompletableFuture.supplyAsync(() -> launcherState.map(state -> state.process().map(process -> {
            log.info("Shutting down webcam app process");
            try {
                process.destroy();
                boolean terminatedGracefully = false;
                try {
                    terminatedGracefully = process.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn("Thread got interrupted at shutdown", e);
                    Thread.currentThread().interrupt(); // Restore interrupted state
                }

                if (process.isAlive()) {
                    log.warn("Stopping webcam app process gracefully did not terminate it. We destroy it forcibly.");
                    process.destroyForcibly();
                    try {
                        process.waitFor(2, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        log.warn("Thread got interrupted while waiting after forced shutdown", e);
                        Thread.currentThread().interrupt();
                    }
                    terminatedGracefully = false;
                }
                return terminatedGracefully;
            } finally {
                state.logReader().ifPresent(WebcamProcessLogReader::shutdown);
            }
        }).orElse(true)).orElse(true), commonForkJoinPool());
    }

    private synchronized LauncherState setStartingState() {
        if (launcherState.isPresent()) {
            throw new IllegalStateException("Webcam app process launch already active");
        }
        LauncherState state = LauncherState.starting();
        launcherState = Optional.of(state);
        return state;
    }

    private synchronized Optional<LauncherState> setRunningState(LauncherState startingState, Process process, WebcamProcessLogReader logReader) {
        if (launcherState.filter(state -> state == startingState).isEmpty()) {
            return Optional.empty();
        }
        LauncherState state = startingState.running(process, logReader);
        launcherState = Optional.of(state);
        return Optional.of(state);
    }

    private synchronized boolean isCurrentLauncherState(LauncherState expectedState) {
        return launcherState.filter(state -> state == expectedState).isPresent();
    }

    private void ensureCurrentLauncherState(LauncherState expectedState) {
        if (!isCurrentLauncherState(expectedState)) {
            throw new CancellationException("Webcam app process launch was cancelled");
        }
    }

    private void clearFailedStartupState(Process process, Optional<WebcamProcessLogReader> logReader, Optional<LauncherState> runningState) {
        if (runningState.isPresent()) {
            getAndClearLauncherStateIfCurrent(runningState.get()).ifPresent(state -> {
                destroyFailedStartupProcess(process);
                state.logReader().ifPresent(WebcamProcessLogReader::shutdown);
            });
        } else {
            destroyFailedStartupProcess(process);
            logReader.ifPresent(WebcamProcessLogReader::shutdown);
        }
    }

    private synchronized Optional<LauncherState> getAndClearLauncherState() {
        Optional<LauncherState> currentState = launcherState;
        launcherState = Optional.empty();
        return currentState;
    }

    private synchronized Optional<LauncherState> getAndClearLauncherStateIfCurrent(LauncherState expectedState) {
        Optional<LauncherState> currentState = launcherState;
        if (currentState.filter(state -> state == expectedState).isEmpty()) {
            return Optional.empty();
        }
        launcherState = Optional.empty();
        return currentState;
    }

    private record LauncherState(Optional<Process> process, Optional<WebcamProcessLogReader> logReader) {
        static LauncherState starting() {
            return new LauncherState(Optional.empty(), Optional.empty());
        }

        LauncherState running(Process process, WebcamProcessLogReader logReader) {
            return new LauncherState(Optional.of(process), Optional.of(logReader));
        }
    }
}
