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

public class Bi2pJarProviderTest {
    private static final String VERSION = "1.0.0";
    private static final String JAR_FILE_NAME = "bi2p-" + VERSION + "-all.jar";
    private static final String RESOURCE_PATH = "bi2p/bi2p-" + VERSION + ".zip";

    @TempDir
    Path tempDir;

    @Test
    void extractsPackagedJarWhenExtractedJarIsMissing() throws IOException {
        byte[] jarBytes = "packaged-jar".getBytes();
        Bi2pJarProvider provider = newProvider(jarBytes);

        Path jarPath = provider.prepareBi2pJar(VERSION);

        assertEquals(tempDir.resolve(JAR_FILE_NAME), jarPath);
        assertArrayEquals(jarBytes, Files.readAllBytes(jarPath));
    }

    @Test
    void keepsExtractedJarWhenItMatchesPackagedJar() throws IOException {
        byte[] jarBytes = "packaged-jar".getBytes();
        Path jarPath = tempDir.resolve(JAR_FILE_NAME);
        Files.write(jarPath, jarBytes);
        Bi2pJarProvider provider = newProvider(jarBytes);
        long lastModified = Files.getLastModifiedTime(jarPath).toMillis();

        Path preparedJarPath = provider.prepareBi2pJar(VERSION);

        assertEquals(jarPath, preparedJarPath);
        assertArrayEquals(jarBytes, Files.readAllBytes(jarPath));
        assertEquals(lastModified, Files.getLastModifiedTime(jarPath).toMillis());
    }

    @Test
    void replacesJarOfAnEarlierReleaseWithTheSameBi2pVersion() throws IOException {
        // The bi2p version stays the same across releases of Bisq, so the jar of an earlier release has the same name
        Path jarPath = tempDir.resolve(JAR_FILE_NAME);
        Files.write(jarPath, "jar-of-an-earlier-release".getBytes());
        byte[] jarBytes = "jar-of-this-release".getBytes();
        Bi2pJarProvider provider = newProvider(jarBytes);

        Path preparedJarPath = provider.prepareBi2pJar(VERSION);

        assertEquals(jarPath, preparedJarPath);
        assertArrayEquals(jarBytes, Files.readAllBytes(jarPath));
    }

    @Test
    void throwsWhenPackagedZipDoesNotContainExpectedJar() throws IOException {
        Bi2pJarProvider provider = newProviderForZipBytes(zipWithEntryBytes("readme.txt", "content".getBytes()));

        assertThrows(IOException.class, () -> provider.prepareBi2pJar(VERSION));
    }

    private Bi2pJarProvider newProvider(byte[] jarBytes) throws IOException {
        return newProviderForZipBytes(zipWithEntryBytes(JAR_FILE_NAME, jarBytes));
    }

    private Bi2pJarProvider newProviderForZipBytes(byte[] zipBytes) {
        return new Bi2pJarProvider(tempDir) {
            @Override
            InputStream openBi2pZipResource(String resourcePath) {
                assertEquals(RESOURCE_PATH, resourcePath);
                return new ByteArrayInputStream(zipBytes);
            }
        };
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
