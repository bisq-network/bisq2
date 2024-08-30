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

package bisq.tor.installer;

import bisq.common.archive.ZipFileExtractionFailedException;
import bisq.common.archive.ZipFileExtractor;
import bisq.common.platform.OS;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class TorBinaryZipExtractor {

    private static final String ARCHIVE_FILENAME = "tor.zip";

    private final File destDir;

    public TorBinaryZipExtractor(File destDir) {
        this.destDir = destDir;
    }

    public void extractBinary() throws IOException {
        InputStream zipFileInputStream = openBinaryZipAsStream();
        try (ZipFileExtractor zipFileExtractor = new ZipFileExtractor(zipFileInputStream, destDir)) {
            zipFileExtractor.extractArchive();
            makeTorBinaryExecutable();
        }
    }

    private InputStream openBinaryZipAsStream() {
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(ARCHIVE_FILENAME);
        if (inputStream == null) {
            throw new ZipFileExtractionFailedException("Couldn't open resource: " + ARCHIVE_FILENAME);
        }
        return inputStream;
    }

    private void makeTorBinaryExecutable() {
        if (!isMacOsOrLinux()) {
            return;
        }

        File torBinaryFile = new File(destDir, "tor");
        if (!torBinaryFile.setExecutable(true)) {
            throw new ZipFileExtractionFailedException("Couldn't make tor binary executable: " + ARCHIVE_FILENAME);
        }
    }

    private boolean isMacOsOrLinux() {
        return OS.isMacOs() || OS.isLinux();
    }
}
