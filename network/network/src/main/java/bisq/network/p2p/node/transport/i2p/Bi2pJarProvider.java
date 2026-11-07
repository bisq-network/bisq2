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
import bisq.common.archive.ZipFileExtractor;
import bisq.common.file.FileReaderUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static bisq.common.archive.ZipEntryVerifier.fileMatchesZipEntry;

/**
 * Provides the bi2p jar in the I2P router directory.
 * The jar is extracted from the zip in the resources again whenever it differs from the jar in that zip. The file name
 * of the jar contains only the version of bi2p, which does not change with every release of Bisq. Checking only that
 * the file exists would keep the jar of the first start after every update.
 */
@Slf4j
class Bi2pJarProvider {
    private final Path i2pRouterDirPath;

    Bi2pJarProvider(Path i2pRouterDirPath) {
        this.i2pRouterDirPath = i2pRouterDirPath;
    }

    Path prepareBi2pJar() throws IOException {
        String version = FileReaderUtils.readStringFromResource("bi2p/version.txt").trim();
        return prepareBi2pJar(version);
    }

    Path prepareBi2pJar(String version) throws IOException {
        String jarFileName = "bi2p-" + version + "-all.jar";
        Path jarFilePath = i2pRouterDirPath.resolve(jarFileName);
        String resourcePath = "bi2p/bi2p-" + version + ".zip";

        if (DevMode.isDevMode() || !fileMatchesZipEntry(jarFilePath, openBi2pZipResource(resourcePath), jarFileName)) {
            extractBi2pZip(resourcePath);
            if (!fileMatchesZipEntry(jarFilePath, openBi2pZipResource(resourcePath), jarFileName)) {
                throw new IOException("Extracted bi2p jar verification failed");
            }
        }
        return jarFilePath;
    }

    private void extractBi2pZip(String resourcePath) throws IOException {
        try (InputStream inputStream = openBi2pZipResource(resourcePath);
             ZipFileExtractor zipFileExtractor = new ZipFileExtractor(inputStream, i2pRouterDirPath)) {
            zipFileExtractor.extractArchive();
            log.info("Extracted zip {} to {}", resourcePath, i2pRouterDirPath);
        }
    }

    InputStream openBi2pZipResource(String resourcePath) throws IOException {
        return FileReaderUtils.getResourceAsStream(resourcePath);
    }
}
