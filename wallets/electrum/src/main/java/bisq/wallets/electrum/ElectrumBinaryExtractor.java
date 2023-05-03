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

package bisq.wallets.electrum;

import bisq.common.archive.ZipFileExtractor;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ElectrumBinaryExtractor {

    public static final String LINUX_BINARY_SUFFIX = "AppImage";
    public static final String MAC_OS_BINARY_SUFFIX = "app";
    public static final String WINDOWS_BINARY_SUFFIX = "exe";

    private static final String ARCHIVE_FILENAME = "electrum-binaries.zip";

    private final File destDir;

    public ElectrumBinaryExtractor(Path destDirPath) {
        this.destDir = destDirPath.toFile();
    }

    public void extractArchive() {
        try {
            createDestDirIfNotPresent();

            try (InputStream inputStream = openBinariesZipAsStream()) {
                if (OsUtils.isMac()) {
                    extractElectrumAppFileToDataDir(inputStream);

                } else {
                    try (ZipFileExtractor zipFileExtractor = new ZipFileExtractor(inputStream, destDir)) {
                        zipFileExtractor.extractArchive();
                    }
                }
            }

        } catch (IOException e) {
            throw new ElectrumExtractionFailedException("Couldn't extract Electrum binary.", e);
        }
    }

    private void createDestDirIfNotPresent() {
        try {
            FileUtils.makeDirs(destDir);
        } catch (IOException e) {
            throw new ElectrumExtractionFailedException("Couldn't create directory: " + destDir, e);
        }
    }

    private InputStream openBinariesZipAsStream() {
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(ARCHIVE_FILENAME);
        if (inputStream == null) {
            throw new ElectrumExtractionFailedException("Couldn't open resource: " + ARCHIVE_FILENAME);
        }
        return inputStream;
    }

    private void extractElectrumAppFileToDataDir(InputStream inputStream) {
        deleteElectrumAppFileIfExisting();
        copyZipFromResourcesToDataDir(inputStream);
        unpackZipFileWithUnzipCommand();
    }

    private void deleteElectrumAppFileIfExisting() {
        File electrumAppInDataDir = new File(destDir, "Electrum.app");
        if (electrumAppInDataDir.exists()) {
            try {
                FileUtils.deleteFileOrDirectory(electrumAppInDataDir);
            } catch (IOException e) {
                log.error("Could not delete " + electrumAppInDataDir, e);
                throw new IllegalStateException("Couldn't delete old Electrum.app", e);
            }
        }
    }

    private void copyZipFromResourcesToDataDir(InputStream inputStream) {
        try {
            File zipFileInDataDir = new File(destDir, ARCHIVE_FILENAME);
            Files.copy(inputStream, zipFileInDataDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ElectrumExtractionFailedException(
                    "Couldn't copy Electrum binaries zip file from resources to the data directory.", e
            );
        }
    }

    private void unpackZipFileWithUnzipCommand() {
        try {
            Process extractProcess = new ProcessBuilder("unzip", "-o", ARCHIVE_FILENAME)
                    .directory(destDir)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            boolean isSuccess = extractProcess.waitFor(30, TimeUnit.SECONDS);
            if (!isSuccess) {
                throw new ElectrumExtractionFailedException("Could not copy Electrum.app to data directory.");
            }
        } catch (InterruptedException | IOException e) {
            throw new ElectrumExtractionFailedException("Could not copy Electrum.app to data directory.");
        }
    }
}
