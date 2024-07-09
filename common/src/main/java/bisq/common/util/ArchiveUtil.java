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

package bisq.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveUtil {
    public static void extractZipFile(File zippedFile, String destinationDirectoryPath) {
        File destinationDirectory = new File(destinationDirectoryPath);
        byte[] buffer = new byte[1024];
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zippedFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destinationDirectory, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    // write file content
                    FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                    int lenght;
                    while ((lenght = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, lenght);
                    }
                    fileOutputStream.close();
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static File newFile(File destinationDirectory, ZipEntry zipEntry) throws IOException {
        File destinationFile = new File(destinationDirectory, zipEntry.getName());
        String destDirPath = destinationDirectory.getCanonicalPath();
        String destFilePath = destinationFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destinationFile;
    }
}
