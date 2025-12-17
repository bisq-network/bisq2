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

import bisq.common.facades.FacadeProvider;
import lombok.extern.slf4j.Slf4j;
import net.i2p.client.I2PSessionException;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKeyFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class I2PKeyUtils {
    public static void writeDestination(I2PKeyPair i2pKeyPair, Path storageDirPath, String tag) {
        Path i2pPrivateKeyDirPath = I2PKeyGeneration.getDestinationFilePath(storageDirPath, tag, "");
        Path destination_b64Path = I2PKeyGeneration.getDestinationFilePath(storageDirPath, tag, "destination_b64");
        Path destination_b32Path = I2PKeyGeneration.getDestinationFilePath(storageDirPath, tag, "destination_b32");
        Path identityBase64Path = I2PKeyGeneration.getDestinationFilePath(storageDirPath, tag, "identity_b64");
        try {
            FacadeProvider.getJdkFacade().createDirectories(i2pPrivateKeyDirPath);
            FacadeProvider.getJdkFacade().writeString(i2pKeyPair.getDestinationBase64(), destination_b64Path);
            FacadeProvider.getJdkFacade().writeString(i2pKeyPair.getDestinationBase32(), destination_b32Path);
            FacadeProvider.getJdkFacade().writeString(Base64.encode(i2pKeyPair.getIdentityBytes()), identityBase64Path);
            log.info("Persisted the I2P private key and destinations into {}", i2pPrivateKeyDirPath);
        } catch (Exception e) {
            log.error("Could not persist I2P destination files", e);
        }
    }

    public static I2PKeyPair fromIdentityBase64(String identityBase64) {
        byte[] identityBytes = Base64.decode(identityBase64);
        Destination destination = destinationFromIdentityBytes(identityBytes);
        return new I2PKeyPair(identityBytes, destination);
    }

    public static Destination destinationFromIdentityBytes(byte[] identityBytes) {
        try (ByteArrayInputStream identityBytesStream = new ByteArrayInputStream(identityBytes)) {
            PrivateKeyFile keyFile = new PrivateKeyFile(identityBytesStream);
            return keyFile.getDestination();
        } catch (IOException | I2PSessionException | DataFormatException e) {
            log.error("Could not resolve destination.", e);
            throw new RuntimeException(e);
        }
    }
}
