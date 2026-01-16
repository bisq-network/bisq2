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

import bisq.common.file.FileReaderUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZipFileExtractorTest {

    @Test
    void testExtractSingleFile(@TempDir Path tempDirPath) throws IOException {
        byte[] zipBytes = createZipWithSingleFile("test.txt", "Hello World");
        try (InputStream is = new ByteArrayInputStream(zipBytes);
             ZipFileExtractor extractor = new ZipFileExtractor(is, tempDirPath)) {
            extractor.extractArchive();
        }
        Path extractedFilePath = tempDirPath.resolve("test.txt");
        assertTrue(Files.exists(extractedFilePath));
        assertEquals("Hello World", FileReaderUtils.readUTF8String(extractedFilePath));
    }

    @Test
    void testExtractDirectoryAndFile(@TempDir Path tempDirPath) throws IOException {
        byte[] zipBytes = createZipWithDirAndFile("dir/", "dir/file.txt", "Data");
        try (InputStream is = new ByteArrayInputStream(zipBytes);
             ZipFileExtractor extractor = new ZipFileExtractor(is, tempDirPath)) {
            extractor.extractArchive();
        }
        assertTrue(Files.isDirectory(tempDirPath.resolve("dir")));
        assertTrue(Files.exists(tempDirPath.resolve("dir/file.txt")));
        assertEquals("Data", FileReaderUtils.readUTF8String(tempDirPath.resolve("dir/file.txt")));
    }

    @Test
    void testThrowExceptionForInvalidZip(@TempDir Path tempDirPath) {
        ByteArrayInputStream invalidZip = new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8));
        try (ZipFileExtractor extractor = new ZipFileExtractor(invalidZip, tempDirPath)) {
            assertThrows(ZipFileExtractionFailedException.class, extractor::extractArchive);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void testThrowExceptionWhenZipEntryEscapesDestDirectory(@TempDir Path tempDirPath) throws IOException {
        // Create a malicious zip with path traversal entry
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("../../../etc/passwd"));
            zos.write("malicious content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        InputStream zipFileInputStream = new ByteArrayInputStream(baos.toByteArray());
        ZipFileExtractor extractor = new ZipFileExtractor(zipFileInputStream, tempDirPath);

        assertThrows(ZipFileExtractionFailedException.class, extractor::extractArchive);
    }

    private byte[] createZipWithSingleFile(String fileName, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    private byte[] createZipWithDirAndFile(String dirName, String fileName, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(dirName));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(fileName));
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}