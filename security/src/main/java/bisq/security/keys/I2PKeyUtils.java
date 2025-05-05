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

package bisq.security.keys;

import bisq.common.file.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class I2PKeyUtils {

    public static void writeDestination(I2PKeyPair i2pKeyPair, Path storageDir, String tag) {
        Path targetPath = Paths.get(storageDir.toString(), tag);
        File i2pPrivateKeyDir = targetPath.toFile();
        try {
            log.info("Storing the I2P private key into {}", i2pPrivateKeyDir);
            FileUtils.makeDirs(i2pPrivateKeyDir);
            String dir = i2pPrivateKeyDir.getAbsolutePath();

            FileUtils.writeToFile(i2pKeyPair.getBase64Destination(), Paths.get(dir, "destination_b64").toFile());

        } catch (Exception e) {
            log.error("Could not persist I2P identity", e);
        }
    }
}
