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

import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ElectrumBinaryExtractor {

    public static final String LINUX_BINARY_SUFFIX = "AppImage";
    public static final String MAC_OS_BINARY_SUFFIX = "app";
    public static final String WINDOWS_BINARY_SUFFIX = "exe";

    private static final String ARCHIVE_FILENAME = "electrum-binaries.zip";

    private final File destDir;

    public ElectrumBinaryExtractor(Path destDirPath) {
        this.destDir = destDirPath.toFile();
    }

    public Path extractFileWithSuffix(String fileNameSuffix) {
        try {
            createDestDirIfNotPresent();

            try (InputStream inputStream = openBinariesZipAsStream()) {
                if (OsUtils.isOSX()) {
                    extractElectrumAppFileToDataDir(inputStream);
                    return destDir.toPath().resolve("Electrum.app");

                } else {
                    File extractedFile = extractFileWithSuffixFromStream(inputStream, fileNameSuffix);
                    return extractedFile.toPath();
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

    private File extractFileWithSuffixFromStream(InputStream inputStream, String fileSuffix) {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {

                String fileName = zipEntry.getName();
                if (fileName.endsWith(fileSuffix)) {
                    return writeStreamToFile(buffer, zipInputStream, fileName);
                }

                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();

        } catch (IOException e) {
            throw new ElectrumExtractionFailedException("Couldn't extract Electrum binary.", e);
        }
        throw new ElectrumExtractionFailedException("Couldn't extract Electrum binary.");
    }

    private void deleteElectrumAppFileIfExisting() {
        File electrumAppInDataDir = new File(destDir, "Electrum.app");
        if (electrumAppInDataDir.exists()) {
            boolean isSuccess = electrumAppInDataDir.delete();
            if (!isSuccess) {
                throw new IllegalStateException("Couldn't delete old Electrum.app");
            }
        }
    }

    private void copyZipFromResourcesToDataDir(InputStream inputStream) {
        try {
            File zipFileInDataDir = new File(destDir, ARCHIVE_FILENAME);
            Files.copy(inputStream, zipFileInDataDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ElectrumExtractionFailedException(
                    "Couldn't copy Electrum binaries zip file from resources to the data directory."
            );
        }
    }

    private void unpackZipFileWithUnzipCommand() {
        try {
            Process extractProcess = new ProcessBuilder("unzip", ARCHIVE_FILENAME)
                    .directory(destDir)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean isSuccess = extractProcess.waitFor(1, TimeUnit.MINUTES);
            if (!isSuccess) {
                throw new ElectrumExtractionFailedException("Could not copy Electrum.app to data directory.");
            }
        } catch (InterruptedException | IOException e) {
            throw new ElectrumExtractionFailedException("Could not copy Electrum.app to data directory.");
        }
    }

    private File writeStreamToFile(byte[] buffer, InputStream inputStream, String fileName) {
        File destFile = new File(destDir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            return destFile;
        } catch (IOException e) {
            throw new ElectrumExtractionFailedException("Couldn't write to stream to: " + destFile);
        }
    }
}
