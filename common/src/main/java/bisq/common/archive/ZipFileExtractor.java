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

import bisq.common.file.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileExtractor implements AutoCloseable {

    private final InputStream zipFileInputStream;
    private final File destDir;

    public ZipFileExtractor(InputStream zipFileInputStream, File destDir) {
        this.zipFileInputStream = zipFileInputStream;
        this.destDir = destDir;
    }

    public void extractArchive() {
        createDirIfNotPresent(destDir);
        extractFiles();
    }

    @Override
    public void close() throws IOException {
        zipFileInputStream.close();
    }

    private void createDirIfNotPresent(File destDir) {
        try {
            FileUtils.makeDirs(destDir);
        } catch (IOException e) {
            throw new ZipFileExtractionFailedException("Couldn't create directory: " + destDir, e);
        }
    }

    private void extractFiles() {
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFileInputStream)) {
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null) {
                String fileName = zipEntry.getName();

                if (zipEntry.isDirectory()) {
                    File dirFile = new File(destDir, fileName);
                    createDirIfNotPresent(dirFile);
                } else {
                    writeStreamToFile(buffer, zipInputStream, fileName);
                }

                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();

        } catch (IOException e) {
            throw new ZipFileExtractionFailedException("Couldn't extract zip file.", e);
        }
    }

    private void writeStreamToFile(byte[] buffer, InputStream inputStream, String fileName) {
        File destFile = new File(destDir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(destFile)) {
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

        } catch (IOException e) {
            throw new ZipFileExtractionFailedException("Couldn't write to stream to: " + destFile);
        }
    }
}
