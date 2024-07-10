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
import bisq.common.util.ArchiveUtil;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebcamProcessLauncher {
    private final String baseDir;
    private final int port;
    private Optional<Process> runningProcess = Optional.empty();

    public WebcamProcessLauncher(String baseDir, int port) {
        this.baseDir = baseDir;
        this.port = port;
    }

    public CompletableFuture<Process> start() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String version = FileUtils.readStringFromResource("webcam-app/version.txt");
                WebcamProcessLauncher.class.getResourceAsStream("");
                String jarFilePath = baseDir + "/webcam-" + version + "-all.jar";
                File jarFile = new File(jarFilePath);

                if (!jarFile.exists() || DevMode.isDevMode()) {
                    String zipFileName = "webcam-" + version + ".zip";
                    File tempFile = new File(baseDir + "/" + zipFileName);
                    FileUtils.resourceToFile("webcam-app/" + zipFileName, tempFile);
                    ArchiveUtil.extractZipFile(tempFile, baseDir);
                    FileUtils.deleteFile(tempFile);
                    log.info("Extracted zip file {} to {}", zipFileName, baseDir);
                }

                String portParam = "--port=" + port;
                String logFileParam = "--logFile=" + URLEncoder.encode(baseDir, StandardCharsets.UTF_8) + FileUtils.FILE_SEP + "webcam-app";
                String pathToJavaExe = System.getProperty("java.home") + "/bin/java";
                log.info("pathToJavaExe {}", pathToJavaExe);
                ProcessBuilder processBuilder;
                if (OsUtils.isMac()) {
                    String iconPath = baseDir + "/webcam-app-icon.png";
                    File bisqIcon = new File(iconPath);
                    if (!bisqIcon.exists()) {
                        FileUtils.resourceToFile("images/webcam/webcam-app-icon@2x.png", bisqIcon);
                    }
                    String jvmArgs = "-Xdock:icon=" + iconPath;
                    processBuilder = new ProcessBuilder(pathToJavaExe, jvmArgs, "-jar", jarFilePath, portParam, logFileParam);
                } else {
                    processBuilder = new ProcessBuilder(pathToJavaExe, "-jar", jarFilePath, portParam, logFileParam);
                }

                Process process = processBuilder.start();
                runningProcess = Optional.of(process);
                log.info("Process successful launched: {}; port={}", process, port);
                return process;
            } catch (Exception e) {
                log.error("Launching process failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdown() {
        log.info("Process shutdown. runningProcess={}", runningProcess);
        runningProcess.ifPresent(process -> {
            process.destroy();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
            }
            if (process.isAlive()) {
                log.warn("Stopping webcam app process gracefully did not terminate it. We destroy it forcibly.");
                process.destroyForcibly();
            }
        });
    }
}
