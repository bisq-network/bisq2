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

package bisq.desktop.i2p_router;

import bisq.common.application.DevMode;
import bisq.common.archive.ZipFileExtractor;
import bisq.common.file.FileUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class I2pRouterProcessLauncher {
    private final String i2pRouterDir;
    private Optional<Process> runningProcess = Optional.empty();

    public I2pRouterProcessLauncher(String baseDir) {
        this.i2pRouterDir = baseDir + "/webcam";
    }

    public CompletableFuture<Process> start( ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String version = FileUtils.readStringFromResource("i2p-router-app/version.txt");
                String jarFilePath = i2pRouterDir + "/i2p-router-app-" + version + "-all.jar";
                File jarFile = new File(jarFilePath);

                if (!jarFile.exists() || DevMode.isDevMode()) {
                    String resourcePath = "i2p-router-app/i2p-router-app-" + version + ".zip";
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                    File destDir = new File(i2pRouterDir);
                    ZipFileExtractor zipFileExtractor = new ZipFileExtractor(inputStream, destDir);
                    zipFileExtractor.extractArchive();
                    log.info("Extracted zip file {} to {}", resourcePath, i2pRouterDir);
                }

                String logFileParam = "--logFile=" + URLEncoder.encode(i2pRouterDir, StandardCharsets.UTF_8) + FileUtils.FILE_SEP + "i2p-router-app";
                String languageParam = "--language=" + LanguageRepository.getDefaultLanguage();

                String pathToJavaExe = System.getProperty("java.home") + "/bin/java";
                ProcessBuilder processBuilder;
                if (OS.isMacOs()) {
                    String iconPath = i2pRouterDir + "/i2p-router-app-icon.png";
                    File bisqIcon = new File(iconPath);
                    if (!bisqIcon.exists()) {
                        FileUtils.resourceToFile("images/webcam/i2p-router-app-icon@2x.png", bisqIcon);
                    }
                    String jvmArgs = "-Xdock:icon=" + iconPath;
                    processBuilder = new ProcessBuilder(pathToJavaExe, jvmArgs, "-jar", jarFilePath, logFileParam, languageParam);
                } else {
                    processBuilder = new ProcessBuilder(pathToJavaExe, "-jar", jarFilePath, logFileParam, languageParam);
                }
                log.info("ProcessBuilder commands: {}", processBuilder.command());
                Process process = processBuilder.start();
                runningProcess = Optional.of(process);
                log.info("Process successful launched: {}; port={}", process);
                return process;
            } catch (Exception e) {
                log.error("Launching process failed", e);
                throw new RuntimeException(e);
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("I2pRouterProcessLauncher"));
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
                log.warn("Stopping i2p router app process gracefully did not terminate it. We destroy it forcibly.");
                process.destroyForcibly();
                terminatedGraceFully = false;
            }
            return terminatedGraceFully;
        }).orElse(true));
    }
}
