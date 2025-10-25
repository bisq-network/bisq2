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

import bisq.common.application.DevMode;
import bisq.common.archive.ZipFileExtractor;
import bisq.common.file.FileUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j
public class WebcamProcessLauncher {
    private final Path webcamDirPath;
    private Optional<Process> runningProcess = Optional.empty();

    public WebcamProcessLauncher(Path appDataDirPath) {
        this.webcamDirPath = appDataDirPath.resolve("webcam");
    }

    public CompletableFuture<Process> start(int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String version = FileUtils.readStringFromResource("webcam-app/version.txt");
                Path jarFilePath = webcamDirPath.resolve("webcam-app-" + version + "-all.jar");

                if (!Files.exists(jarFilePath) || DevMode.isDevMode()) {
                    String resourcePath = "webcam-app/webcam-app-" + version + ".zip";
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                    ZipFileExtractor zipFileExtractor = new ZipFileExtractor(inputStream, webcamDirPath);
                    zipFileExtractor.extractArchive();
                    log.info("Extracted zip file {} to {}", resourcePath, webcamDirPath);
                }

                String portParam = "--port=" + port;
                String logFileParam = "--logFile=" + URLEncoder.encode(webcamDirPath.toAbsolutePath().toString(), StandardCharsets.UTF_8) + FileUtils.FILE_SEP + "webcam-app";
                String languageTagParam = "--languageTag=" + LanguageRepository.getDefaultLanguageTag();

                String pathToJavaExe = System.getProperty("java.home") + "/bin/java";
                ProcessBuilder processBuilder;
                if (OS.isMacOs()) {
                    String iconPath = webcamDirPath + "/webcam-app-icon.png";
                    Path bisqIconPath = Path.of(iconPath);
                    if (!Files.exists(bisqIconPath)) {
                        FileUtils.resourceToFile("images/webcam/webcam-app-icon@2x.png", bisqIconPath);
                    }
                    String jvmArgs = "-Xdock:icon=" + iconPath;
                    processBuilder = new ProcessBuilder(pathToJavaExe, jvmArgs, "-jar", jarFilePath.toAbsolutePath().toString(), portParam, logFileParam, languageTagParam);
                } else {
                    processBuilder = new ProcessBuilder(pathToJavaExe, "-jar", jarFilePath.toAbsolutePath().toString(), portParam, logFileParam, languageTagParam);
                }
                log.info("ProcessBuilder commands: {}", processBuilder.command());
                Process process = processBuilder.start();
                runningProcess = Optional.of(process);
                log.info("Process successful launched: {}; port={}", process, port);
                return process;
            } catch (Exception e) {
                log.error("Launching process failed", e);
                throw new RuntimeException(e);
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("WebcamProcessLauncher"));
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.supplyAsync(() -> runningProcess.map(process -> {
            log.info("Process shutdown. runningProcess={}", runningProcess);
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
