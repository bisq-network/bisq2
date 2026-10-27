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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Checks whether a file which was extracted from a zip file earlier still has the content of the entry in the zip file.
 * Apps which extract a packaged jar only once, and reuse it at later starts, use it to detect an extracted jar of an
 * older version or a changed jar.
 */
public class ZipEntryVerifier {
    public static boolean fileMatchesZipEntry(Path filePath, InputStream zipInputStream, String entryFileName) throws IOException {
        try (zipInputStream) {
            if (!Files.exists(filePath)) {
                return false;
            }
            return MessageDigest.isEqual(sha256(filePath), sha256ZipEntry(zipInputStream, entryFileName));
        }
    }

    static byte[] sha256(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return sha256(inputStream);
        }
    }

    static byte[] sha256ZipEntry(InputStream zipInputStream, String entryFileName) throws IOException {
        try (ZipInputStream inputStream = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = inputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && isEntry(zipEntry, entryFileName)) {
                    return sha256(inputStream);
                }
            }
        }
        throw new IOException("Entry not found in zip: " + entryFileName);
    }

    private static boolean isEntry(ZipEntry zipEntry, String entryFileName) {
        String entryName = zipEntry.getName();
        return entryName.equals(entryFileName) || entryName.endsWith("/" + entryFileName);
    }

    private static byte[] sha256(InputStream inputStream) throws IOException {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform has to support SHA-256
            throw new IllegalStateException(e);
        }
        // We do not close the DigestInputStream, as this would close the ZipInputStream of the caller
        new DigestInputStream(inputStream, messageDigest).transferTo(OutputStream.nullOutputStream());
        return messageDigest.digest();
    }
}
