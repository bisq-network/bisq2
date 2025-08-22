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
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;

import java.nio.file.Path;

@Slf4j
public class I2PKeyUtils {
    public static void writeDestination(I2PKeyPair i2pKeyPair, Path storageDir, String tag) {
        Path i2pPrivateKeyDir = I2PKeyGeneration.getDestinationFilePath(storageDir, tag, "");
        Path destination_b64Path = I2PKeyGeneration.getDestinationFilePath(storageDir, tag, "destination_b64");
        Path destination_b32Path = I2PKeyGeneration.getDestinationFilePath(storageDir, tag, "destination_b32");
        try {
            log.info("Storing the I2P private key into {}", i2pPrivateKeyDir);
            FileUtils.makeDirs(i2pPrivateKeyDir);
            FileUtils.writeToFile(i2pKeyPair.getDestinationBase64(), destination_b64Path.toFile());
            FileUtils.writeToFile(i2pKeyPair.getDestinationBase32(), destination_b32Path.toFile());
        } catch (Exception e) {
            log.error("Could not persist I2P destination files", e);
        }
    }

    public static I2PKeyPair fromDestinationBase64(String destinationBase64) {
        try {
            Destination destination = new Destination(destinationBase64);
            return new I2PKeyPair(destination);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
