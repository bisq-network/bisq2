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

package bisq.webcam;

import bisq.common.application.ApplicationVersion;
import bisq.common.util.OsUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
                String osName = OsUtils.getOSName().replaceAll(" ", "");
                String version = ApplicationVersion.getVersion().getVersionAsString();
                WebcamProcessLauncher.class.getResourceAsStream("");

                // String jarFilePath = "/Users/dev/IdeaProjects/bisq2/apps/desktop/webcam/build/libs/webcam-" + version + "-" + osName + "-all.jar";
                String jarFilePath = baseDir + "/webcam-" + version + "-" + osName + "-all.jar";
                // jarFilePath = getClass().getResource("/jar/" + jarFilePath).getPath();
                log.info("jarFilePath={} {}", jarFilePath);

                //jarFilePath=file:<HOME_DIR>/Downloads/Bisq%202.app/Contents/app/webcam-2.0.4.jar!/jar/webcam-2.0.4-macosx-all.jar {}

                String portParam = "--port=" + port;
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", jarFilePath, portParam);

              /*  processBuilder.redirectErrorStream(true);
                File errorLogFile = new File(Path.of(baseDir,"webcam-launcher-error.log").toAbsolutePath().toString());
                if(!errorLogFile.exists()){
                    errorLogFile.createNewFile();
                }
                processBuilder.redirectError(errorLogFile);*/

              /*  File logFile = Path.of(baseDir,"webcam-launcher.log").toFile();
                if(!logFile.exists()){
                    logFile.createNewFile();
                }
                processBuilder.redirectOutput(logFile);*/

                Process process = processBuilder.start();
                runningProcess = Optional.of(process);
                log.info("Process successful launched: {}", process);
                return process;
            } catch (Exception e) {
                log.error("Launching process failed", e);
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdown() {
        log.info("Process shutdown. runningProcess={}", runningProcess);
        runningProcess.ifPresent(Process::destroy);
    }
}
