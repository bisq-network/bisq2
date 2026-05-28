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

import bisq.security.DigestUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class WebcamJarVerifier {
    static boolean jarMatchesPackagedZip(Path jarFilePath, InputStream packagedZipInputStream, String jarFileName) throws IOException {
        try (packagedZipInputStream) {
            if (!Files.exists(jarFilePath)) {
                return false;
            }
            return MessageDigest.isEqual(sha256(jarFilePath), sha256ZipEntry(packagedZipInputStream, jarFileName));
        }
    }

    static byte[] sha256(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return DigestUtil.sha256(inputStream);
        }
    }

    static byte[] sha256ZipEntry(InputStream zipInputStream, String jarFileName) throws IOException {
        try (ZipInputStream inputStream = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = inputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && isJarEntry(zipEntry, jarFileName)) {
                    return DigestUtil.sha256(inputStream);
                }
            }
        }
        throw new IOException("Packaged webcam jar not found in zip: " + jarFileName);
    }

    private static boolean isJarEntry(ZipEntry zipEntry, String jarFileName) {
        String entryName = zipEntry.getName();
        return entryName.equals(jarFileName) || entryName.endsWith("/" + jarFileName);
    }

}
