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
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static bisq.desktop.webcam.WebcamJarVerifier.jarMatchesPackagedZip;

@Slf4j
class WebcamJarProvider {
    private final Path webcamDirPath;

    WebcamJarProvider(Path webcamDirPath) {
        this.webcamDirPath = webcamDirPath;
    }

    Path prepareWebcamJar() throws IOException {
        String version = FileReaderUtils.readStringFromResource("webcam-app/version.txt").trim();
        return prepareWebcamJar(version);
    }

    Path prepareWebcamJar(String version) throws IOException {
        String jarFileName = "webcam-app-" + version + "-all.jar";
        Path jarFilePath = webcamDirPath.resolve(jarFileName);
        String resourcePath = "webcam-app/webcam-app-" + version + ".zip";

        if (DevMode.isDevMode() || !jarMatchesPackagedZip(jarFilePath, openWebcamZipResource(resourcePath), jarFileName)) {
            extractWebcamZip(resourcePath);
            if (!jarMatchesPackagedZip(jarFilePath, openWebcamZipResource(resourcePath), jarFileName)) {
                throw new IOException("Extracted webcam jar verification failed");
            }
        }
        return jarFilePath;
    }

    private void extractWebcamZip(String resourcePath) throws IOException {
        try (InputStream inputStream = openWebcamZipResource(resourcePath);
             ZipFileExtractor zipFileExtractor = new ZipFileExtractor(inputStream, webcamDirPath)) {
            zipFileExtractor.extractArchive();
            log.info("Extracted webcam app resources");
        }
    }

    InputStream openWebcamZipResource(String resourcePath) throws IOException {
        return FileReaderUtils.getResourceAsStream(resourcePath);
    }
}
