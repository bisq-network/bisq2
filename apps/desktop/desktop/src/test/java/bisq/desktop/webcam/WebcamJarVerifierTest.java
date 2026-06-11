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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebcamJarVerifierTest {
    private static final String JAR_FILE_NAME = "webcam-app-1.0.0-all.jar";

    @TempDir
    Path tempDir;

    @Test
    void returnsTrueWhenExtractedJarMatchesPackagedJar() throws IOException {
        byte[] jarBytes = "jar-content".getBytes();
        Path jarPath = tempDir.resolve(JAR_FILE_NAME);
        Files.write(jarPath, jarBytes);

        boolean matches = WebcamJarVerifier.jarMatchesPackagedZip(jarPath, zipWithJar(jarBytes), JAR_FILE_NAME);

        assertTrue(matches);
    }

    @Test
    void returnsFalseWhenExtractedJarDiffersFromPackagedJar() throws IOException {
        Path jarPath = tempDir.resolve(JAR_FILE_NAME);
        Files.write(jarPath, "tampered-content".getBytes());

        boolean matches = WebcamJarVerifier.jarMatchesPackagedZip(jarPath, zipWithJar("jar-content".getBytes()), JAR_FILE_NAME);

        assertFalse(matches);
    }

    @Test
    void returnsFalseWhenExtractedJarIsMissing() throws IOException {
        Path missingJarPath = tempDir.resolve(JAR_FILE_NAME);

        boolean matches = WebcamJarVerifier.jarMatchesPackagedZip(missingJarPath, zipWithJar("jar-content".getBytes()), JAR_FILE_NAME);

        assertFalse(matches);
    }

    @Test
    void throwsWhenPackagedZipDoesNotContainExpectedJar() {
        assertThrows(IOException.class,
                () -> WebcamJarVerifier.sha256ZipEntry(zipWithEntry("readme.txt", "content".getBytes()), JAR_FILE_NAME));
    }

    private InputStream zipWithJar(byte[] jarBytes) throws IOException {
        return zipWithEntry("nested/" + JAR_FILE_NAME, jarBytes);
    }

    private InputStream zipWithEntry(String entryName, byte[] bytes) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(bytes);
            zipOutputStream.closeEntry();
        }
        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
