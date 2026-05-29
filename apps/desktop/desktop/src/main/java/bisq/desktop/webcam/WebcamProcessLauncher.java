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
import bisq.common.file.FileReaderUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public class WebcamProcessLauncher {
    private final Path webcamDirPath;
    private final WebcamJarProvider webcamJarProvider;
    private Optional<Process> runningProcess = Optional.empty();

    public WebcamProcessLauncher(Path appDataDirPath) {
        this.webcamDirPath = appDataDirPath.resolve("webcam");
        this.webcamJarProvider = new WebcamJarProvider(webcamDirPath);
    }

    public CompletableFuture<Process> start(int port, String sessionSecret) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path jarFilePath = webcamJarProvider.prepareWebcamJar();

                String portParam = "--port=" + port;
                String logFileParam = "--logFile=" + URLEncoder.encode(webcamDirPath.toAbsolutePath().toString(), StandardCharsets.UTF_8) + FileReaderUtils.FILE_SEP + "webcam-app";
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
                    processBuilder = new ProcessBuilder(pathToJavaExe, jvmArgs, "-jar", jarFilePath.toAbsolutePath().toString(), portParam, logFileParam, languageTagParam);
                } else {
                    processBuilder = new ProcessBuilder(pathToJavaExe, "-jar", jarFilePath.toAbsolutePath().toString(), portParam, logFileParam, languageTagParam);
                }
                processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
                log.info("Launching webcam app process");
                Process process = processBuilder.start();
                sendSessionSecret(process, sessionSecret);
                runningProcess = Optional.of(process);
                log.info("Webcam app process successfully launched");
                return process;
            } catch (Exception e) {
                log.error("Launching process failed", e);
                throw new RuntimeException(e);
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("WebcamProcessLauncher"));
    }

    private void sendSessionSecret(Process process, String sessionSecret) {
        // Child stdin is reserved for this one-shot IPC secret bootstrap.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(sessionSecret);
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            process.destroyForcibly();
            throw new RuntimeException("Sending webcam IPC session secret failed", e);
        }
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.supplyAsync(() -> runningProcess.map(process -> {
            log.info("Shutting down webcam app process");
            process.destroy();
            boolean terminatedGraceFully = false;
            try {
                terminatedGraceFully = process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("Thread got interrupted at shutdown", e);
                Thread.currentThread().interrupt(); // Restore interrupted state
            }

            if (process.isAlive()) {
                log.warn("Stopping webcam app process gracefully did not terminate it. We destroy it forcibly.");
                process.destroyForcibly();
                terminatedGraceFully = false;
            }
            return terminatedGraceFully;
        }).orElse(true), commonForkJoinPool());
    }
}
