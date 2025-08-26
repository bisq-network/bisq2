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
import bisq.common.file.FileUtils;
import bisq.common.locale.LanguageRepository;
import bisq.common.platform.OS;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class I2pRouterProcessLauncher implements Service {

    private final String i2cpHost;
    private final int i2cpPort;
    private final Path i2pRouterDir;

    public I2pRouterProcessLauncher(String i2cpHost, int i2cpPort, Path i2pDirPath) {
        this.i2cpHost = i2cpHost;
        this.i2cpPort = i2cpPort;
        this.i2pRouterDir = i2pDirPath.resolve("i2p-router").toAbsolutePath();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String version = FileUtils.readStringFromResource("i2p-router-app/version.txt");
                Path jarFilePath = i2pRouterDir.resolve("i2p-router-app-" + version + "-all.jar");

                // Extract ZIP if jar missing or dev mode
                if (!Files.exists(jarFilePath) || DevMode.isDevMode()) {
                    String resourcePath = "i2p-router-app/i2p-router-app-" + version + ".zip";
                    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                        if (inputStream == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
                        new ZipFileExtractor(inputStream, i2pRouterDir.toFile()).extractArchive();
                    }
                    log.info("Extracted zip {} to {}", resourcePath, i2pRouterDir);
                }

                // JVM & app arguments
                List<String> command = new ArrayList<>();
                command.add("nohup"); // detach on Unix
                String javaExe = System.getProperty("java.home") + "/bin/java";
                command.add(javaExe);

                if (OS.isMacOs()) {
                    Path iconPath = i2pRouterDir.resolve("i2p-router-app-icon.png");
                    if (!Files.exists(iconPath)) {
                        FileUtils.resourceToFile("images/i2p_router/i2p-router-app-icon@2x.png", iconPath.toFile());
                    }
                    command.add("-Xdock:icon=" + iconPath);
                }

                command.add("-jar");
                command.add(jarFilePath.toString());

              /*  Runtime.getRuntime().exec(new String[]{
                        "nohup",javaExe, "-jar", jarFilePath.toString()
                });
               */

                command.add("--i2pRouterDir=" + i2pRouterDir);
                command.add("--i2cpHost=" + i2cpHost);
                command.add("--i2cpPort=" + i2cpPort);
                command.add("--language=" + LanguageRepository.getDefaultLanguage());

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(i2pRouterDir.toFile());
                processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

                log.info("Launching I2P router with command: {}", processBuilder.command());
                Process process = processBuilder.start();

                log.info("I2P router launched: pid={}, process={}", process.pid(), process);
                return true;
            } catch (Exception e) {
                log.error("Failed to launch I2P router process", e);
                throw new RuntimeException(e);
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("I2pRouterProcessLauncher"));
    }
}
