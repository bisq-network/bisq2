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

package bisq.network.p2p.node.transport.i2p;

import bisq.common.application.DevMode;
import bisq.common.application.Service;
import bisq.common.archive.ZipFileExtractor;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class Bi2pProcessLauncher implements Service {

    private final String i2cpHost;
    private final int i2cpPort;
    private final String bi2pGrpcHost;
    private final int bi2pGrpcPort;
    private final Path i2pRouterDirPath;
    private final String httpProxyHost;
    private final int httpProxyPort;
    private final boolean httpProxyEnabled;
    private ScheduledExecutorService executor;

    public Bi2pProcessLauncher(String i2cpHost,
                               int i2cpPort,
                               String bi2pGrpcHost,
                               int bi2pGrpcPort,
                               Path i2pRouterDirPath,
                               String httpProxyHost,
                               int httpProxyPort,
                               boolean httpProxyEnabled) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.bi2pGrpcHost = bi2pGrpcHost;
        this.bi2pGrpcPort = bi2pGrpcPort;
        this.i2pRouterDirPath = i2pRouterDirPath;
        this.httpProxyHost = httpProxyHost;
        this.httpProxyPort = httpProxyPort;
        this.httpProxyEnabled = httpProxyEnabled;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        executor = ExecutorFactory.newSingleThreadScheduledExecutor("Bi2pProcessLauncher");
        return CompletableFuture.supplyAsync(() -> {
            try {
                String version = FileReaderUtils.readStringFromResource("bi2p/version.txt").trim();
                Path jarFilePath = i2pRouterDirPath.resolve("bi2p-" + version + "-all.jar");

                // Extract ZIP if jar missing or dev mode
                if (!Files.exists(jarFilePath) || DevMode.isDevMode()) {
                    String resourcePath = "bi2p/bi2p-" + version + ".zip";
                    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                        if (inputStream == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
                        new ZipFileExtractor(inputStream, i2pRouterDirPath).extractArchive();
                    }
                    log.info("Extracted zip {} to {}", resourcePath, i2pRouterDirPath);
                }

                // JVM & app arguments
                List<String> command = new ArrayList<>();

                if (OS.isUnix()) {
                    // Ignores the SIGHUP signal that is normally sent to child processes when the host process closes.
                    command.add("nohup"); // OSX and Linux

                    if (OS.isLinux()) {
                        // Creates a new session and process group, which detaches the process from the controlling process.
                        // Main purpose: the process won’t receive SIGHUP when the parent terminal or app exits
                        command.add("setsid"); // Linux only
                    }
                }

                command.add(getJavaExePath());

                if (OS.isMacOs()) {
                    Path iconPath = i2pRouterDirPath.resolve("bi2p-app_512.png");
                    if (!Files.exists(iconPath)) {
                        FileMutatorUtils.resourceToFile("images/bi2p/bi2p-app_512.png", iconPath);
                    }
                    command.add("-Xdock:icon=" + iconPath);
                }

                command.add("-jar");
                command.add(jarFilePath.toString());
                command.add("--i2pRouterDir=" + i2pRouterDirPath);
                command.add("--i2cpHost=" + i2cpHost);
                command.add("--i2cpPort=" + i2cpPort);
                command.add("--bi2pGrpcHost=" + bi2pGrpcHost);
                command.add("--bi2pGrpcPort=" + bi2pGrpcPort);
                command.add("--httpProxyHost=" + httpProxyHost);
                command.add("--httpProxyPort=" + httpProxyPort);
                command.add("--httpProxyEnabled=" + httpProxyEnabled);
                command.add("--languageTag=" + LanguageRepository.getDefaultLanguageTag());

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(i2pRouterDirPath.toFile());

                Path stderrLogPath = i2pRouterDirPath.resolve("bi2p_err.log");
                processBuilder.redirectError(stderrLogPath.toFile());
                // We have any the log file from the Bi2p router, so we discard.
                processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                // processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

                log.info("Launching I2P router with command: {}", processBuilder.command());
                Process process = processBuilder.start();
                log.info("I2P router launched with pid {}", process.pid());
                return true;
            } catch (Exception e) {
                log.error("Failed to launch I2P router process", e);
                throw new RuntimeException(e);
            }
        }, executor).whenComplete((r, t) -> {
            ExecutorFactory.shutdownAndAwaitTermination(executor);
            executor = null;
        });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (executor != null) {
            ExecutorFactory.shutdownAndAwaitTermination(executor);
        }
        return CompletableFuture.completedFuture(true);
    }

    private static String getJavaExePath() {
        String javaHome = System.getProperty("java.home");
        String exe = OS.isWindows() ? "java.exe" : "java";
        return Paths.get(javaHome, "bin", exe).toString();
    }

}
