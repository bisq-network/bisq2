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

package bisq.common.archive;

import bisq.common.file.FileMutatorUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileExtractor implements AutoCloseable {

    private final InputStream zipFileInputStream;
    private final Path destPath;

    public ZipFileExtractor(InputStream zipFileInputStream, Path destPath) {
        this.zipFileInputStream = zipFileInputStream;
        this.destPath = destPath;
    }

    public void extractArchive() {
        createDirIfNotPresent(destPath);
        extractFiles();
    }

    @Override
    public void close() throws IOException {
        zipFileInputStream.close();
    }

    private void createDirIfNotPresent(Path destDirPath) {
        try {
            FileMutatorUtils.createDirectories(destDirPath);
        } catch (IOException e) {
            throw new ZipFileExtractionFailedException("Couldn't create directory: " + destDirPath, e);
        }
    }

    private void extractFiles() {
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFileInputStream)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            if (zipEntry == null) {
                throw new IOException("Invalid or empty zip file");
            }

            Path normalizedDestPath = destPath.normalize();
            do {
                Path targetPath = destPath.resolve(zipEntry.getName()).normalize();
                // Security check: prevent path traversal attacks
                if (!targetPath.startsWith(normalizedDestPath)) {
                    throw new IOException("Entry is outside of the target directory");
                }
                if (zipEntry.isDirectory()) {
                    FileMutatorUtils.createDirectories(targetPath);
                } else {
                    // Ensure parent directories exist
                    FileMutatorUtils.createDirectories(targetPath.getParent());
                    // Copy stream content to file
                    FileMutatorUtils.inputStreamToFile(zipInputStream, targetPath);
                }
            } while ((zipEntry = zipInputStream.getNextEntry()) != null);
        } catch (IOException e) {
            throw new ZipFileExtractionFailedException("Couldn't extract zip file.", e);
        }
    }
}
