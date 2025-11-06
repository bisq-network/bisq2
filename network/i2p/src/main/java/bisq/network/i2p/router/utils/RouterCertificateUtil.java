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

package bisq.network.i2p.router.utils;

import bisq.common.file.FileMutatorUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RouterCertificateUtil {
    public static void copyCertificatesFromResources(Path i2pDirPath) throws IOException, URISyntaxException {
        Path certDirPath = createDirectory(i2pDirPath, "certificates");
        Path seedDirPath = createDirectory(certDirPath, "reseed");
        Path sslDirPath = createDirectory(certDirPath, "ssl");
        FileMutatorUtils.copyResourceDirectory("certificates/reseed/", seedDirPath);
        FileMutatorUtils.copyResourceDirectory("certificates/ssl/", sslDirPath);
    }

    private static Path createDirectory(Path parentPath, String child) throws IOException {
        Path dirPath = parentPath.resolve(child);
        Files.createDirectories(dirPath);
        return dirPath;
    }
}
