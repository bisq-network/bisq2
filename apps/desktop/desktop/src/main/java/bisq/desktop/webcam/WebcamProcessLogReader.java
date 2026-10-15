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

import bisq.common.logging.LogSetup;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

@Slf4j
class WebcamProcessLogReader {
    private static final String WEBCAM_PROCESS_LOGGER_NAME = "bisq.desktop.webcam.WebcamProcess";

    private final InputStream inputStream;
    private final ExecutorService executorService;
    private final Logger webcamProcessLogger;

    private volatile boolean isStopped;

    private WebcamProcessLogReader(InputStream inputStream, Path logFilePath) throws IOException {
        this.inputStream = inputStream;
        this.executorService = ExecutorFactory.newSingleThreadExecutor("WebcamProcessLogReader");
        Files.createDirectories(logFilePath.getParent());
        webcamProcessLogger = LogSetup.setupRawRollingLogger(WEBCAM_PROCESS_LOGGER_NAME, logFilePath.toAbsolutePath().toString());
    }

    static WebcamProcessLogReader start(InputStream inputStream, Path logFilePath) throws IOException {
        WebcamProcessLogReader reader = new WebcamProcessLogReader(inputStream, logFilePath);
        reader.start();
        return reader;
    }

    void shutdown() {
        isStopped = true;
        try {
            inputStream.close();
        } catch (IOException ignore) {
        }
        executorService.shutdownNow();
    }

    private void start() {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while (!isStopped && (line = reader.readLine()) != null) {
                    webcamProcessLogger.info(StringUtils.maskHomeDirectory(line));
                }
            } catch (IOException e) {
                if (!isStopped) {
                    log.warn("Reading webcam app stderr failed", e);
                }
            } finally {
                executorService.shutdown();
            }
        });
    }
}
