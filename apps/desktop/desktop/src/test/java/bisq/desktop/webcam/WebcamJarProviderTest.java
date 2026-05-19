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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebcamJarProviderTest {
    private static final String VERSION = "1.0.0";
    private static final String JAR_FILE_NAME = "webcam-app-" + VERSION + "-all.jar";
    private static final String RESOURCE_PATH = "webcam-app/webcam-app-" + VERSION + ".zip";

    @TempDir
    Path tempDir;

    @Test
    void extractsPackagedJarWhenExtractedJarIsMissing() throws IOException {
        byte[] jarBytes = "packaged-jar".getBytes();
        WebcamJarProvider provider = newProvider(jarBytes);

        Path jarPath = provider.prepareWebcamJar(VERSION);

        assertEquals(tempDir.resolve(JAR_FILE_NAME), jarPath);
        assertArrayEquals(jarBytes, Files.readAllBytes(jarPath));
    }

    @Test
    void keepsExtractedJarWhenItMatchesPackagedJar() throws IOException {
        byte[] jarBytes = "packaged-jar".getBytes();
        Path jarPath = tempDir.resolve(JAR_FILE_NAME);
        Files.write(jarPath, jarBytes);
        WebcamJarProvider provider = newProvider(jarBytes);
        long lastModified = Files.getLastModifiedTime(jarPath).toMillis();

        Path preparedJarPath = provider.prepareWebcamJar(VERSION);

        assertEquals(jarPath, preparedJarPath);
        assertArrayEquals(jarBytes, Files.readAllBytes(jarPath));
        assertEquals(lastModified, Files.getLastModifiedTime(jarPath).toMillis());
    }

    @Test
    void replacesExtractedJarWhenItDiffersFromPackagedJar() throws IOException {
        byte[] jarBytes = "packaged-jar".getBytes();
        Path jarPath = tempDir.resolve(JAR_FILE_NAME);
        Files.write(jarPath, "tampered-jar".getBytes());
        WebcamJarProvider provider = newProvider(jarBytes);

        provider.prepareWebcamJar(VERSION);

        assertArrayEquals(jarBytes, Files.readAllBytes(jarPath));
    }

    @Test
    void throwsWhenPackagedZipDoesNotContainExpectedJar() throws IOException {
        WebcamJarProvider provider = newProviderForZipBytes(zipWithEntryBytes("readme.txt", "content".getBytes()));

        assertThrows(IOException.class, () -> provider.prepareWebcamJar(VERSION));
    }

    private WebcamJarProvider newProvider(byte[] jarBytes) throws IOException {
        return newProviderForZipBytes(zipWithJarBytes(jarBytes));
    }

    private WebcamJarProvider newProviderForZipBytes(byte[] zipBytes) {
        return new WebcamJarProvider(tempDir) {
            @Override
            InputStream openWebcamZipResource(String resourcePath) {
                assertEquals(RESOURCE_PATH, resourcePath);
                return new ByteArrayInputStream(zipBytes);
            }
        };
    }

    private byte[] zipWithJarBytes(byte[] jarBytes) throws IOException {
        return zipWithEntryBytes(JAR_FILE_NAME, jarBytes);
    }

    private byte[] zipWithEntryBytes(String entryName, byte[] bytes) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(bytes);
            zipOutputStream.closeEntry();
        }
        return byteArrayOutputStream.toByteArray();
    }
}
